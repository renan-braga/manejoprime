#!/bin/sh
set -eu

env_file="${1:-.env}"
compose_file="${2:-compose.yml}"

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

backup_root="${BACKUP_ROOT:-/var/backups/curralzap}"
retention_days="${POSTGRES_BACKUP_RETENTION_DAYS:-14}"
timestamp="$(date -u '+%Y%m%dT%H%M%SZ')"
backup_parent="${backup_root%/}/postgres"
backup_dir="${backup_parent}/${timestamp}"
metadata_file="${backup_dir}/metadata.json"
latest_link="${backup_parent}/latest"

require_value "POSTGRES_DB" "${POSTGRES_DB:-}"
require_value "EVOLUTION_POSTGRES_DB" "${EVOLUTION_POSTGRES_DB:-}"
require_value "POSTGRES_USER" "${POSTGRES_USER:-}"
require_value "POSTGRES_PASSWORD" "${POSTGRES_PASSWORD:-}"

case "$retention_days" in
  ''|*[!0-9]*)
    echo "POSTGRES_BACKUP_RETENTION_DAYS must be a non-negative integer" >&2
    exit 1
    ;;
esac

mkdir -p "$backup_dir"

dump_database() {
  database_name="$1"
  output_file="$2"
  temp_sql_file="${backup_dir}/.${database_name}.sql"

  docker compose --env-file "$env_file" -f "$compose_file" exec -T \
    postgres \
    env PGPASSWORD="$POSTGRES_PASSWORD" \
    pg_dump \
      -U "$POSTGRES_USER" \
      -d "$database_name" \
      --clean \
      --if-exists \
      --no-owner \
      --no-privileges > "$temp_sql_file"

  gzip -c "$temp_sql_file" > "$output_file"
  rm -f "$temp_sql_file"
}

curralzap_dump="${backup_dir}/${POSTGRES_DB}.sql.gz"
evolution_dump="${backup_dir}/${EVOLUTION_POSTGRES_DB}.sql.gz"

dump_database "$POSTGRES_DB" "$curralzap_dump"
dump_database "$EVOLUTION_POSTGRES_DB" "$evolution_dump"

curralzap_size="$(wc -c < "$curralzap_dump" | tr -d ' ')"
evolution_size="$(wc -c < "$evolution_dump" | tr -d ' ')"

cat <<EOF > "$metadata_file"
{
  "createdAt": "${timestamp}",
  "backupRoot": "${backup_root}",
  "backupDirectory": "${backup_dir}",
  "retentionDays": ${retention_days},
  "databases": [
    {
      "name": "${POSTGRES_DB}",
      "path": "${curralzap_dump}",
      "sizeBytes": ${curralzap_size}
    },
    {
      "name": "${EVOLUTION_POSTGRES_DB}",
      "path": "${evolution_dump}",
      "sizeBytes": ${evolution_size}
    }
  ]
}
EOF

ln -sfn "$backup_dir" "$latest_link"

find "$backup_parent" \
  -mindepth 1 \
  -maxdepth 1 \
  -type d \
  -mtime +"$retention_days" \
  -exec rm -rf {} +

echo "Created PostgreSQL backups in $backup_dir" >&2
printf '%s\n' "$backup_dir"
