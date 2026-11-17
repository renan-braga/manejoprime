#!/bin/sh
set -eu

env_file="${1:-.env}"
compose_file="${2:-compose.yml}"
dump_path="${3:-}"
source_database="${4:-}"

if [ -z "$dump_path" ]; then
  echo "Usage: $0 [env_file] [compose_file] dump_path [source_database]" >&2
  exit 1
fi

if [ ! -f "$dump_path" ]; then
  echo "Dump file not found: $dump_path" >&2
  exit 1
fi

set -a
. "$env_file"
set +a

require_value() {
  name="$1"
  value="$2"

  if [ -z "$value" ]; then
    echo "Missing required value: $name" >&2
    exit 1
  fi
}

require_value "POSTGRES_USER" "${POSTGRES_USER:-}"
require_value "POSTGRES_PASSWORD" "${POSTGRES_PASSWORD:-}"

if [ -z "$source_database" ]; then
  source_database="$(basename "$dump_path" .sql.gz)"
fi

sanitized_source="$(printf '%s' "$source_database" | tr -c '[:alnum:]' '_' | tr '[:upper:]' '[:lower:]')"
verify_db="restore_verify_${sanitized_source}_$(date -u '+%Y%m%d%H%M%S')"
temp_sql_file="$(mktemp)"

compose_exec() {
  docker compose --env-file "$env_file" -f "$compose_file" exec -T "$@"
}

drop_verify_db() {
  compose_exec \
    postgres \
    env PGPASSWORD="$POSTGRES_PASSWORD" \
    psql \
      -v ON_ERROR_STOP=1 \
      -U "$POSTGRES_USER" \
      -d postgres \
      -c "DROP DATABASE IF EXISTS \"$verify_db\" WITH (FORCE);" >/dev/null 2>&1 || true
}

cleanup() {
  drop_verify_db
  rm -f "$temp_sql_file"
}

trap cleanup EXIT INT TERM

drop_verify_db

compose_exec \
  postgres \
  env PGPASSWORD="$POSTGRES_PASSWORD" \
  psql \
    -v ON_ERROR_STOP=1 \
    -U "$POSTGRES_USER" \
    -d postgres \
    -c "CREATE DATABASE \"$verify_db\";"

gzip -dc "$dump_path" > "$temp_sql_file"

compose_exec \
  postgres \
  env PGPASSWORD="$POSTGRES_PASSWORD" \
  psql \
    -v ON_ERROR_STOP=1 \
    -U "$POSTGRES_USER" \
    -d "$verify_db" < "$temp_sql_file" >/dev/null

table_count="$(
  compose_exec \
    postgres \
    env PGPASSWORD="$POSTGRES_PASSWORD" \
    psql \
      -At \
      -v ON_ERROR_STOP=1 \
      -U "$POSTGRES_USER" \
      -d "$verify_db" \
      -c "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public';" | tr -d '[:space:]'
)"

case "$table_count" in
  ''|*[!0-9]*)
    echo "Restore verification returned an invalid table count for $dump_path: ${table_count:-empty}" >&2
    exit 1
    ;;
esac

if [ "$table_count" -lt 1 ]; then
  echo "Restore verification found no public tables in $dump_path" >&2
  exit 1
fi

echo "Verified restore for $dump_path into $verify_db with $table_count public tables" >&2
