#!/bin/sh
set -eu

create_database() {
  database_name="$1"

  if [ -z "${database_name}" ]; then
    return
  fi

  existing_count="$(psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres --tuples-only --no-align <<SQL
SELECT COUNT(*) FROM pg_database WHERE datname = '${database_name}';
SQL
)"

  if [ "${existing_count}" = "0" ]; then
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<SQL
CREATE DATABASE "${database_name}";
SQL
  fi
}

create_database "${EVOLUTION_POSTGRES_DB:-}"
