# jPrime – production deployment (Docker Compose)

One server, one `docker compose up`. Everything is built from source inside Docker – the server needs only
Docker Engine 24+ with the Compose plugin (no JDK, Maven or Node).

```
Internet ──80/443──> caddy ──┬── jprime.io        ──> site  :8080  (Qute public site)
                    (HTTPS)  │     /api/visitor|ticket|speaker/** ──> admin  (legacy check-in apps)
                             └── admin.jprime.io  ──> admin :8081  (admin API + bundled React UI)
                                       site, admin ──> db  (MySQL 8.4, internal network only)
                                       backup ──> daily mysqldump into deploy/backups/
```

| File | Purpose |
|---|---|
| `../Dockerfile` | Multi-stage build: React UI → Maven (core, site, admin) → two runtime images (`--target site` / `--target admin`), Temurin 21 JRE + fonts for captcha/JasperReports, non-root, health checks |
| `docker-compose.yml` | The stack: `db`, `site`, `admin`, `caddy`, `backup` |
| `caddy/Caddyfile` | TLS (Let's Encrypt), security headers, gzip/zstd, hides `/q/*`, optional admin IP allow-list |
| `mysql/conf.d/jprime.cnf` | utf8mb4, `log-bin-trust-function-creators` (Flyway creates stored procedures), BLOB packet size |
| `.env.example` | All settings and secrets → copy to `.env` |
| `scripts/init-env.sh` | Creates `.env` with random secrets |
| `scripts/restore-db.sh` | Imports a dump (e.g. the legacy production DB) |
| `scripts/backup-loop.sh` | Run by the `backup` container |

## 1. Prepare the server

* Linux with Docker Engine + Compose plugin (`docker compose version`), ≥ 2 GB RAM (4 GB recommended for building).
* DNS: `A`/`AAAA` records for `jprime.io` and `admin.jprime.io` (or your staging names) pointing at the server.
* Firewall: open 80/tcp, 443/tcp, 443/udp. Nothing else needs to be public (MySQL is not published).

```bash
git clone https://github.com/dalexandrov/jprime.git /opt/jprime
cd /opt/jprime/deploy
./scripts/init-env.sh        # .env with random DB passwords, session keys, captcha secret, admin password
nano .env                    # SITE_DOMAIN, ADMIN_DOMAIN, ACME_EMAIL, MAIL_PASSWORD (+ MAIL_TEST_ADDRESS on staging)
```

## 2. Load the production data (recommended before going live)

Start only the database, import the legacy dump, then start the rest:

```bash
docker compose up -d db
./scripts/restore-db.sh ~/jprime-prod.sql.gz     # drops & recreates "jprime", imports, (re)starts site + admin
```

The dump should be of the `jprime` schema (e.g. `mysqldump --single-transaction --routines jprime`). If it contains
`USE <other name>`, remove that line. On start-up Flyway validates the existing `flyway_schema_history` and only adds
`V1.7.0` (a no-op on the production schema). Skip this step to start with an empty database (Flyway creates the
schema and the 2015–2025 branches).

## 3. Start

```bash
docker compose up -d --build       # first build takes a few minutes (Maven + npm downloads)
docker compose ps                  # all services "healthy" / "running"
docker compose logs -f site admin  # wait for "Listening on: http://0.0.0.0:8080" / ":8081"
```

Open `https://<SITE_DOMAIN>` and `https://<ADMIN_DOMAIN>` (login `admin` / `ADMIN_PASSWORD`). Caddy fetches the
certificates on the first request; if that fails, check `docker compose logs caddy` (DNS or ports 80/443).

For staging use different domains and set `MAIL_TEST_ADDRESS` (all mails go there) or `MAIL_MOCK=true`.

## Updating

```bash
cd /opt/jprime && git pull
cd deploy && docker compose up -d --build site admin
```

Downtime is the restart of each app (~10 s).

## Backups

The `backup` service writes `backups/jprime-YYYYMMDD-HHMMSS.sql.gz` every day at `BACKUP_HOUR` and keeps
`BACKUP_KEEP_DAYS` days. **Copy them off the server** (rsync/rclone cron job). Also worth keeping: `.env` and the
`caddy_data` volume (certificates).

```bash
docker compose exec backup bash /scripts/backup-loop.sh now     # backup right now
./scripts/restore-db.sh backups/jprime-20261022-030000.sql.gz    # restore one
```

## Operations cheat sheet

```bash
docker compose ps                                  # status + health
docker compose logs -f --tail=200 site             # logs (rotated: 5 x 20 MB per container)
docker compose restart admin                       # restart one app
docker compose exec db sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" jprime'   # SQL shell
docker compose down                                # stop (data stays in the mysql_data volume)
docker system prune -f                             # free space from old images after updates
```

## Security notes

* Secrets live only in `.env` (`chmod 600`, git-ignored). `ADMIN_PASSWORD` can be a BCrypt hash – write `$` as `$$`.
* Restrict the admin UI to known IPs with `ADMIN_ALLOWED_IPS` (Caddy answers 403 to everyone else). The check-in
  API on the main domain is not affected (it uses HTTP Basic with the admin credentials).
* `/q/*` (health, OpenAPI) is not reachable from outside; the containers' own health checks use it internally.
* MySQL is on an internal Docker network without internet access and no published port; use the SQL shell from
  the cheat sheet (or `docker compose exec backup mysql -h db -uroot -p jprime`) instead of exposing it.
* Changing `SITE_SESSION_KEY` / `ADMIN_SESSION_KEY` logs everybody out; changing `CAPTCHA_SECRET` only invalidates
  captchas that are currently displayed.

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `set XYZ in .env` when running compose | a required variable is empty – fill it in `.env` |
| app restarts, log says `Validate failed: Migrations have failed validation` | the imported DB has a different Flyway history – check `flyway_schema_history` against `core/src/main/resources/db/migration` |
| `You do not have the SUPER privilege and binary logging is enabled` | `mysql/conf.d/jprime.cnf` not mounted – run compose from the `deploy/` directory |
| captcha / PDF invoice errors about fonts | you are not running the images from this Dockerfile (they install fontconfig + DejaVu) |
| `OutOfMemoryError` / container killed | raise `SITE_MEM_LIMIT` / `ADMIN_MEM_LIMIT` (the JVM uses 75 % of it) |
| certificates not issued | DNS does not point at the server yet, or 80/443 blocked; Caddy retries automatically |
