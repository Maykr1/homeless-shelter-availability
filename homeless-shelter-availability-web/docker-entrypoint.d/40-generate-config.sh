#!/bin/sh
set -eu

TEMPLATE="/usr/share/nginx/html/config.template.js"
OUTPUT="/usr/share/nginx/html/config.js"

read_existing_value() {
  key="$1"
  file="$2"

  if [ ! -f "$file" ]; then
    return 0
  fi

  sed -n "s/.*${key}: \"\\([^\"]*\\)\".*/\\1/p" "$file" | head -n 1
}

existing_google_maps_api_key="$(read_existing_value GOOGLE_MAPS_API_KEY "$OUTPUT")"
existing_api_base_url="$(read_existing_value API_BASE_URL "$OUTPUT")"

: "${GOOGLE_MAPS_API_KEY:=${existing_google_maps_api_key:-}}"
: "${API_BASE_URL:=${existing_api_base_url:-http://localhost:8081}}"

if [ -f "$TEMPLATE" ]; then
  envsubst '${GOOGLE_MAPS_API_KEY} ${API_BASE_URL}' < "$TEMPLATE" > "$OUTPUT"
fi
