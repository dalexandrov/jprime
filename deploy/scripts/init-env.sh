#!/usr/bin/env bash
# Creates deploy/.env from .env.example with freshly generated random secrets.
# You still have to set the domains and MAIL_PASSWORD afterwards.
set -euo pipefail
cd "$(dirname "$0")/.."
[[ -e .env ]] && { echo ".env already exists - not touching it" >&2; exit 1; }

rnd() { openssl rand -base64 48 | tr -d '/+=\n' | cut -c1-"${1:-32}"; }

cp .env.example .env
for key in MYSQL_ROOT_PASSWORD MYSQL_PASSWORD SITE_SESSION_KEY ADMIN_SESSION_KEY CAPTCHA_SECRET ADMIN_PASSWORD; do
  sed -i.bak "s|^${key}=change-me\$|${key}=$(rnd 32)|" .env
done
rm -f .env.bak
chmod 600 .env
echo "Created .env with random secrets. Now edit it: domains, MAIL_PASSWORD."
echo "Admin login: admin / $(grep '^ADMIN_PASSWORD=' .env | cut -d= -f2-)"
