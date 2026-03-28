#!/bin/sh
set -eu

TEMPLATE="/usr/share/nginx/html/config.template.js"
OUTPUT="/usr/share/nginx/html/config.js"

: "${GOOGLE_MAPS_API_KEY:=}"
: "${API_BASE_URL:=http://localhost:8081}"

if [ -f "$TEMPLATE" ]; then
  envsubst '${GOOGLE_MAPS_API_KEY} ${API_BASE_URL}' < "$TEMPLATE" > "$OUTPUT"
fi
