#!/usr/bin/env bash
# Generate config.js from local .env file
# Usage: ./generate-config.sh [local|production]

set -euo pipefail

MODE="${1:-local}"
ENV_FILE="env/${MODE}/.env"
OUTPUT="webApp/src/main/resources/config.js"

if [[ ! -f "$ENV_FILE" ]]; then
    echo "Error: $ENV_FILE not found"
    echo "Create it from env/${MODE}/.env.template"
    exit 1
fi

# Read env file and export variables
export SUPABASE_URL=$(grep '^SUPABASE_URL=' "$ENV_FILE" | cut -d'=' -f2-)
export SUPABASE_ANON_KEY=$(grep '^SUPABASE_ANON_KEY=' "$ENV_FILE" | cut -d'=' -f2-)

if [[ -z "$SUPABASE_URL" ]] || [[ -z "$SUPABASE_ANON_KEY" ]]; then
    echo "Error: Missing SUPABASE_URL or SUPABASE_ANON_KEY in $ENV_FILE"
    exit 1
fi

# Generate config.js
cat > "$OUTPUT" <<EOF
// Generated from env/${MODE}/.env - DO NOT COMMIT
window.FUEL_CONFIG = {
  SUPABASE_URL: "$SUPABASE_URL",
  SUPABASE_ANON_KEY: "$SUPABASE_ANON_KEY"
};
EOF

echo "Generated $OUTPUT from $ENV_FILE"