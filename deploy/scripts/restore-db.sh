#!/usr/bin/env bash
# Restores a dump (plain .sql or .sql.gz) into the jprime database, e.g. the legacy production dump.
#
#   ./scripts/restore-db.sh /path/to/jprime-prod.sql.gz
#
# The apps are stopped during the import and started again afterwards; Flyway then validates the schema.
# A legacy dump made without --databases is fine too: it is imported into "jprime".
set -euo pipefail
cd "$(dirname "$0")/.."

dump="${1:?usage: $0 <dump.sql|dump.sql.gz>}"
[[ -f "$dump" ]] || { echo "No such file: $dump" >&2; exit 1; }

read -rp "This REPLACES the current jprime database with $dump. Continue? [y/N] " ok
[[ "$ok" == [yY] ]] || exit 1

docker compose stop site admin
docker compose up -d db
until docker compose exec -T db sh -c 'mysqladmin ping -h 127.0.0.1 -uroot -p"$MYSQL_ROOT_PASSWORD" --silent' 2>/dev/null; do sleep 2; done

echo "Recreating database jprime..."
docker compose exec -T db sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "
  DROP DATABASE IF EXISTS jprime;
  CREATE DATABASE jprime CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  GRANT ALL PRIVILEGES ON jprime.* TO \"$MYSQL_USER\"@\"%\";"'

echo "Importing $dump ..."
if [[ "$dump" == *.gz ]]; then gunzip -c "$dump"; else cat "$dump"; fi \
  | docker compose exec -T db sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 jprime'

docker compose up -d site admin
echo "Done. Follow the start-up (Flyway validation) with: docker compose logs -f site admin"
