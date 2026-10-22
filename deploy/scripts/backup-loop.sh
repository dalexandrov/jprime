#!/usr/bin/env bash
# Runs inside the "backup" container: one compressed mysqldump of the jprime database per day at BACKUP_HOUR,
# keeping BACKUP_KEEP_DAYS days. Files land in deploy/backups/ on the host. Copy them off the server regularly!
set -uo pipefail

: "${MYSQL_HOST:=db}" "${BACKUP_HOUR:=3}" "${BACKUP_KEEP_DAYS:=14}"
export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"

dump() {
  local file="/backups/jprime-$(date +%Y%m%d-%H%M%S).sql.gz"
  echo "[backup] $(date -Is) dumping to $file"
  if mysqldump -h "$MYSQL_HOST" -uroot --single-transaction --routines --triggers --events \
       --set-gtid-purged=OFF --default-character-set=utf8mb4 --databases jprime | gzip > "$file.part"; then
    mv "$file.part" "$file"
    echo "[backup] done ($(du -h "$file" | cut -f1))"
  else
    rm -f "$file.part"
    echo "[backup] FAILED" >&2
  fi
  find /backups -name 'jprime-*.sql.gz' -mtime +"$BACKUP_KEEP_DAYS" -print -delete
}

# Manual run: docker compose exec backup bash /scripts/backup-loop.sh now
if [[ "${1:-}" == "now" ]]; then dump; exit; fi

echo "[backup] daily backup at ${BACKUP_HOUR}:00 ($TZ), keeping ${BACKUP_KEEP_DAYS} days"
while true; do
  now=$(date +%s)
  next=$(date -d "today ${BACKUP_HOUR}:00" +%s)
  (( next <= now )) && next=$(date -d "tomorrow ${BACKUP_HOUR}:00" +%s)
  sleep $(( next - now ))
  dump
done
