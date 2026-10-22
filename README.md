# jPrime – Quarkus rewrite

Rewrite of the jPrime conference web application (previously Spring Boot 3.5 + JSP + Thymeleaf) on
**Quarkus 3.40 LTS / Java 21**, reusing the **existing MySQL schema** unchanged.

| Module      | What                                                                 | Tech                                   | Port |
|-------------|----------------------------------------------------------------------|----------------------------------------|------|
| `core`      | JPA entities (1:1 with the existing tables), Flyway scripts, repositories, business services (CFP, tickets, invoices, mails, PDFs, CSV, raffle…) | Hibernate ORM + Panache, Flyway, Mailer, Qute (mails), JasperReports, ZXing | – |
| `site`      | Public web site                                                       | Quarkus REST + **Qute** server-side templates | 8080 |
| `admin`     | Independent admin application: JSON API + legacy check-in API        | Quarkus REST + Jackson, form/basic auth, OpenAPI | 8081 |
| `admin-ui`  | Admin single page application                                         | **React 19 + TypeScript + Vite**, React Router | 5173 (dev) |

## Why this split

* **Public site → Qute (server-side rendering).** The site is mostly read-only content (speakers, agenda,
  articles) that must be indexed by search engines and load fast; server-rendered HTML gives that for free, keeps
  the site in Java and needs no Node build. The few forms (CFP, tickets, sign-up) are plain HTML forms with a
  little vanilla JavaScript.
* **Admin → React SPA + its own Quarkus backend.** The admin is CRUD-heavy and interactive (tables, filters,
  inline editing, uploads, the raffle screen) – a good fit for React. It is a **separate application**: its own
  Quarkus process (own port, own security config, deployable/scalable/restartable independently) and its own UI
  project. It shares only the `core` library and the database with the site.

No theme was applied on purpose: both UIs use a small plain stylesheet (`site/.../css/site.css`,
`admin-ui/src/styles.css`) so the real design can be added later without fighting existing styles.

## Quick start (development)

Prerequisites: JDK 21+, Maven 3.9+, Docker Desktop (for MySQL), Node 22+ (admin UI only).

Use four terminal tabs, each starting in the project root:

```bash
# Tab 1 - database + build (re-run the build only after code changes)
docker compose up -d                       # MySQL 8 on 3306 (root/admin) + Adminer on http://localhost:8180
mvn clean install -DskipTests              # builds core and both apps

# Tab 2 - public site, wait for "Listening on: http://localhost:8080"
mvn quarkus:dev -pl site

# Tab 3 - admin API, wait for "Listening on: http://localhost:8081"
#         (-Ddebug=5006: the site's dev mode already uses debug port 5005)
mvn quarkus:dev -pl admin -Ddebug=5006

# Tab 4 - admin UI (proxies /api, /j_security_check and /q to :8081)
cd admin-ui && npm install && npm run dev  # npm install is needed only the first time
```

Then open:

| What | URL | Login |
|---|---|---|
| Public site | http://localhost:8080 | – |
| Admin UI | http://localhost:5173 | `admin` / `TestPassword` |
| Admin API docs (Swagger UI) | http://localhost:8081/q/swagger-ui | – |
| Adminer (database browser) | http://localhost:8180 | server `db`, user `root`, password `admin` |

The dev admin password is `%dev.jprime.admin.password` in `admin/src/main/resources/application.properties`.

If the admin login says "Invalid username or password", check that the admin API is running:
`curl -i -X POST http://localhost:8081/j_security_check -d j_username=admin -d j_password=TestPassword`
should return `200`. A refused connection means tab 3 is not running. The UI shows the same message for any failed
login, including a backend that is down.

To stop everything, press Ctrl+C in tabs 2-4, then run `docker compose down`. The data stays in the `mysql_data` volume.

Flyway runs the original migration scripts at start-up (from both apps – Flyway locks, so this is safe), creating
the schema in an empty database or validating an existing one. **Point it at a copy of the production database
first** to verify (see "Schema compatibility").

The initial data migration creates branches 2015–2025 with 2025 as the current one. Create/edit the current
edition and its ticket prices in the admin UI → *Branches* (the home page and ticket page need ticket prices).

## Configuration

Shared defaults live in `core/src/main/resources/META-INF/microprofile-config.properties`; each app overrides in its
`application.properties`. In production use environment variables:

| Variable | Used by | Purpose |
|---|---|---|
| `QUARKUS_DATASOURCE_JDBC_URL`, `QUARKUS_DATASOURCE_USERNAME`, `QUARKUS_DATASOURCE_PASSWORD` | both | MySQL |
| `QUARKUS_MAILER_PASSWORD` (and `QUARKUS_MAILER_HOST`/`_USERNAME` if they change) | both | SMTP (mail.jprime.io:465) |
| `JPRIME_MAIL_TEST_ADDRESS` | both | send **all** mails to this address (staging) – replaces `test.email.address` |
| `QUARKUS_HTTP_AUTH_SESSION_ENCRYPTION_KEY` | both (different values) | encrypts the login cookie, ≥ 16 chars |
| `JPRIME_ADMIN_PASSWORD` | admin | password of the `admin` account (plain or BCrypt `$2a$…`) – replaces `admin.password` |
| `JPRIME_ADMIN_EMAILS` | admin | optional: comma separated e-mails of registered users that may log into the admin |
| `JPRIME_CAPTCHA_SECRET` | site | signs the captcha answer; required when running more than one site instance |
| `JPRIME_EPAY_KEY` | core | ePay secret (only if online payments are re-enabled) |

In dev and test mode mails are **not sent** (`quarkus.mailer.mock=true`) – they are logged.

## Building for production

```bash
mvn package                                # site/target/quarkus-app, admin/target/quarkus-app (API only)
mvn package -Pbundle-ui                    # admin jar also serves the React UI (runs npm install + build)
java -jar site/target/quarkus-app/quarkus-run.jar
java -jar admin/target/quarkus-app/quarkus-run.jar
```

`site|admin/src/main/docker/Dockerfile.jvm` are plain Quarkus images for a pre-built jar. For a server, use the
Docker setup below instead.

The admin UI can also be deployed on its own (`npm run build` → static files in `admin-ui/dist`) behind a reverse
proxy that forwards `/api` and `/j_security_check` to the admin app on the same origin (the session cookie is
`SameSite=Strict`).

## Deployment (Docker)

The root `Dockerfile` builds both apps from source (the admin image bundles the React UI; the runtime images contain
the fonts that the captcha, image resizing and JasperReports need). `deploy/docker-compose.yml` runs the whole stack:

```
Internet ──80/443──> caddy ──┬── jprime.io        ──> site  :8080
                    (HTTPS)  │     /api/visitor|ticket|speaker/** ──> admin  (legacy check-in apps)
                             └── admin.jprime.io  ──> admin :8081  (API + React UI)
                                 site, admin ──> db (MySQL 8.4, internal network only)
                                 backup ──> daily mysqldump into deploy/backups/
```

**Server requirements:** Linux with Docker Engine 24+ and the Compose plugin, ≥ 2 GB RAM (4 GB to build on the
server), DNS `A`/`AAAA` records for both domains pointing at it, and ports 80/tcp, 443/tcp, 443/udp open.
No JDK, Maven or Node is needed.

### First deployment

```bash
git clone https://github.com/dalexandrov/jprime.git /opt/jprime
cd /opt/jprime/deploy

./scripts/init-env.sh              # creates .env with random DB passwords, session keys, captcha secret, admin password
nano .env                          # set SITE_DOMAIN, ADMIN_DOMAIN, ACME_EMAIL, MAIL_PASSWORD
                                   # (staging: MAIL_TEST_ADDRESS=you@example.com or MAIL_MOCK=true)

# optional but recommended: start from the production data
docker compose up -d db
./scripts/restore-db.sh ~/jprime-prod.sql.gz

docker compose up -d --build       # first build takes a few minutes
docker compose ps                  # everything "healthy" / "running"
docker compose logs -f site admin  # wait for "Listening on: http://0.0.0.0:8080" / ":8081"
```

Then open `https://<SITE_DOMAIN>` and `https://<ADMIN_DOMAIN>` (login `admin` / the `ADMIN_PASSWORD` from `.env`).
Caddy obtains the Let's Encrypt certificates on the first request. Without a dump, Flyway creates an empty schema
with the 2015–2025 branches; with the production dump it only validates it (see "Schema compatibility").

### Updating

```bash
cd /opt/jprime && git pull
cd deploy && docker compose up -d --build site admin
```

### Day-to-day

```bash
docker compose exec backup bash /scripts/backup-loop.sh now               # backup now (daily backups run at BACKUP_HOUR)
./scripts/restore-db.sh backups/jprime-YYYYMMDD-HHMMSS.sql.gz             # restore a backup
docker compose logs -f --tail=200 site                                    # logs
docker compose exec db sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" jprime'   # SQL shell
docker compose down                                                       # stop (data stays in the mysql_data volume)
```

Copy `deploy/backups/` and `.env` off the server regularly. Use `ADMIN_ALLOWED_IPS` in `.env` to restrict the admin
domain to known IPs. All settings, plus troubleshooting, are in [`deploy/README.md`](deploy/README.md) and
[`deploy/.env.example`](deploy/.env.example).

## Tests

```bash
mvn test                  # unit tests (formats, VAT, ePay signing, captcha, agenda layout, PDF rendering…)
mvn verify -Pdb-tests     # + @QuarkusTest suites against a throw-away MySQL (Dev Services, needs Docker);
                          #   they run the original Flyway scripts and render every public page,
                          #   which also validates the entity mapping against the real schema
```

## Schema compatibility

The schema is **not changed**. Hibernate never touches it (`schema-management.strategy=none`); Flyway uses the
very same scripts, versions and locations, so the existing `flyway_schema_history` validates. One script was added:

* `V1.7.0__create_number_generator_tables.sql` – `CREATE TABLE IF NOT EXISTS` for the three invoice-counter tables
  that the legacy app created through `ddl-auto=update`. On the production DB it is a no-op.

Mapping details worth knowing:

* Naming strategy = `PhysicalNamingStrategyStandardImpl` (camelCase columns), like before; all columns are also
  named explicitly in the entities.
* `User` is a single table hierarchy (`DTYPE` = `User` / `Speaker` / `Sponsor`), unchanged.
* `Branch` keeps its camelCase audit columns (`createdBy`, …) – a side effect of property access in the old entity.
* `Branch.duration` is NUMERIC nanoseconds, `Registrant.paymentType` is an ORDINAL – do not reorder `PaymentType`.
* Invoice numbers are now taken with `SELECT … FOR UPDATE` on the counter row, so the site and the admin app can
  never issue the same number (the old code used a JVM `synchronized` block).

## URL map (legacy → new)

| Legacy | New | Notes |
|---|---|---|
| `/`, `/team`, `/venue`, `/privacy-policy[?bg]`, `/speakers`, `/speaker/{id}`, `/agenda`, `/agenda/{id}`, `/nav…`, `/tickets`, `/cfp`, `/qr`, `/qr/tuk`, `/qr/tukjpro`, `/image/*`, `/captcha-image`, `/pwa/findSessionsByHall`, `/app` | same | |
| `/cfp-thank-you`, `/cfp-problem` | `/cfp/thank-you`, `/cfp/problem` | old URL redirects |
| `/login`, `/signup`, `/resetPassword`, `/createNewPassword`, `/logout` | `/account/login`, `/account/signup`, `/account/reset-password`, `/account/new-password`, `/account/logout` | old URLs redirect |
| `/pwa` (old PWA page) | redirects to `/app` (the Flutter app) | |
| `/admin/**` (JSP) | admin app: React UI + `/api/admin/**` | |
| `/raffle/view`, `/raffle/view/jpro` | admin UI → *Raffle* | |
| `/api/visitor/**`, `/api/ticket/**`, `/api/speaker/**` (check-in app) | same paths on the **admin** app | HTTP Basic, role ADMIN; JSON unchanged (the speaker's `branch` is reduced to `label` + `year`) |
| Spring Data REST endpoints (`/articles`, `/speakers` JSON …) | removed | were exposed by accident |

## Behaviour changes (deliberate)

* No HTTP session: the captcha answer is kept in a signed cookie, logins use an encrypted cookie (Quarkus form auth).
* "Send tickets", "E-mail all visitors" and the "I am here" lookup by name now work on the **current branch** only
  (the old code iterated over the visitors of all years).
* A failed ticket e-mail no longer leaves a new ticket number on the visitor.
* Prices are shown in EUR (Bulgaria uses the euro); the ePay payload currency was changed from BGN to EUR. ePay
  itself was not wired in the legacy UI (bank transfer only) – the signing code is kept in `EpayService`.
* The agenda is rendered as one table per day with a column per hall (configurable: `jprime.agenda.halls`).

## Project layout

```
core/      model/ (entities)  repository/ (Panache)  service/ (business logic)  pdf/ (Jasper + QR)
           src/main/jasperreports (invoice & ticket templates, compiled at build time)
           src/main/resources/db/migration (Flyway)  mail/ (Qute e-mail templates)
site/      web/ (JAX-RS resources rendering Qute templates)  security/  resources/templates  META-INF/resources (images, /app)
admin/     api/ (admin JSON API)  legacy/ (check-in API)  dto/  security/
admin-ui/  src/pages (one page per admin section)  src/components  src/api.ts  src/types.ts
```
