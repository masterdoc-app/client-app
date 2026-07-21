#!/usr/bin/env bash
set -euo pipefail

DEPLOY_PATH="${DEPLOY_PATH:-/opt/fixaverse-app}"
CERTBOT_EMAIL="${CERTBOT_EMAIL:-admin@fixaverse.ru}"
SITE_HOST="${SITE_HOST:-app.fixaverse.ru}"
SITE="/etc/nginx/sites-available/${SITE_HOST}"
WEB_ROOT="/var/www/${SITE_HOST}"

mkdir -p /var/www/certbot "${WEB_ROOT}"

if [[ -f "/etc/letsencrypt/live/${SITE_HOST}/fullchain.pem" ]]; then
  cp "${DEPLOY_PATH}/app.fixaverse.ru.nginx.conf" "${SITE}"
else
  cp "${DEPLOY_PATH}/app.fixaverse.ru.nginx.http.conf" "${SITE}"
fi

ln -sf "${SITE}" "/etc/nginx/sites-enabled/${SITE_HOST}"
nginx -t
systemctl reload nginx

if [[ ! -f "/etc/letsencrypt/live/${SITE_HOST}/fullchain.pem" ]]; then
  certbot certonly --webroot -w /var/www/certbot \
    -d "${SITE_HOST}" \
    --non-interactive --agree-tos --email "${CERTBOT_EMAIL}"
  cp "${DEPLOY_PATH}/app.fixaverse.ru.nginx.conf" "${SITE}"
  nginx -t
  systemctl reload nginx
fi

echo "OK: https://${SITE_HOST}/"
