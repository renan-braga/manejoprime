#!/bin/sh
set -eu

env_file="${1:-.env}"
compose_file="${2:-compose.yml}"

set -a
. "$env_file"
set +a

expected_services="reverse-proxy postgres redis kafka evolution-api backend-api backend-worker admin-web"
healthy_services="postgres redis kafka backend-api backend-worker"

compose_ps() {
  docker compose --env-file "$env_file" -f "$compose_file" ps "$@"
}

container_id_for() {
  compose_ps -q "$1"
}

assert_service_running() {
  service="$1"
  attempt=1
  max_attempts=30

  while [ "$attempt" -le "$max_attempts" ]; do
    if compose_ps --status running --services | grep -qx "$service"; then
      return 0
    fi

    sleep 2
    attempt=$((attempt + 1))
  done

  echo "Service is not running: $service" >&2
  compose_ps >&2
  exit 1
}

service_status() {
  service="$1"
  container_id="$(container_id_for "$service")"
  if [ -z "$container_id" ]; then
    echo "missing"
    return
  fi

  docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container_id"
}

print_service_diagnostics() {
  service="$1"
  container_id="$(container_id_for "$service")"
  if [ -z "$container_id" ]; then
    echo "No container found for $service" >&2
    compose_ps >&2
    return
  fi

  echo "Diagnostics for $service ($container_id):" >&2
  docker inspect --format 'status={{.State.Status}} health={{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}} exit={{.State.ExitCode}} error={{.State.Error}}' "$container_id" >&2
  docker logs --tail 80 "$container_id" >&2 || true
}

wait_for_service_healthy() {
  service="$1"
  attempt=1
  max_attempts=30

  while [ "$attempt" -le "$max_attempts" ]; do
    status="$(service_status "$service")"
    if [ "$status" = "healthy" ]; then
      return 0
    fi

    if [ "$status" = "unhealthy" ]; then
      echo "Service became unhealthy: $service" >&2
      print_service_diagnostics "$service"
      exit 1
    fi

    sleep 2
    attempt=$((attempt + 1))
  done

  echo "Timed out waiting for service health: $service (last status: ${status:-unknown})" >&2
  print_service_diagnostics "$service"
  exit 1
}

wait_for_status() {
  url="$1"
  expected_status="$2"
  attempt=1
  max_attempts=30
  response_file="$(mktemp)"

  while [ "$attempt" -le "$max_attempts" ]; do
    status="$(curl -k -sS -o "$response_file" -w '%{http_code}' "$url" || true)"
    if [ "$status" = "$expected_status" ]; then
      rm -f "$response_file"
      return 0
    fi

    sleep 2
    attempt=$((attempt + 1))
  done

  echo "Expected HTTP $expected_status from $url but last status was ${status:-none}" >&2
  if [ -s "$response_file" ]; then
    echo "Last response body:" >&2
    cat "$response_file" >&2
  fi
  rm -f "$response_file"
  exit 1
}

wait_for_json_response() {
  url="$1"
  expected_status="$2"
  request_body="$3"
  expected_fragment="$4"
  attempt=1
  max_attempts=30
  response_file="$(mktemp)"

  while [ "$attempt" -le "$max_attempts" ]; do
    status="$(
      curl -k -sS -o "$response_file" -w '%{http_code}' \
        -H 'Content-Type: application/json' \
        -d "$request_body" \
        "$url" || true
    )"
    if [ "$status" = "$expected_status" ] && grep -Fq "$expected_fragment" "$response_file"; then
      rm -f "$response_file"
      return 0
    fi

    sleep 2
    attempt=$((attempt + 1))
  done

  echo "Expected HTTP $expected_status from $url with response containing: $expected_fragment" >&2
  echo "Last status was ${status:-none}" >&2
  if [ -s "$response_file" ]; then
    echo "Last response body:" >&2
    cat "$response_file" >&2
  fi
  rm -f "$response_file"
  exit 1
}

wait_for_host_status() {
  host="$1"
  port="$2"
  path="$3"
  expected_status="$4"
  attempt=1
  max_attempts=30
  response_file="$(mktemp)"

  while [ "$attempt" -le "$max_attempts" ]; do
    status="$(
      curl -k -sS --resolve "${host}:${port}:127.0.0.1" \
        -o "$response_file" \
        -w '%{http_code}' \
        "https://${host}:${port}${path}" || true
    )"
    if [ "$status" = "$expected_status" ]; then
      rm -f "$response_file"
      return 0
    fi

    sleep 2
    attempt=$((attempt + 1))
  done

  echo "Expected HTTP $expected_status from https://${host}:${port}${path} but last status was ${status:-none}" >&2
  if [ -s "$response_file" ]; then
    echo "Last response body:" >&2
    cat "$response_file" >&2
  fi
  rm -f "$response_file"
  exit 1
}

wait_for_host_json_response() {
  host="$1"
  port="$2"
  path="$3"
  expected_status="$4"
  request_body="$5"
  expected_fragment="$6"
  attempt=1
  max_attempts=30
  response_file="$(mktemp)"

  while [ "$attempt" -le "$max_attempts" ]; do
    status="$(
      curl -k -sS --resolve "${host}:${port}:127.0.0.1" \
        -o "$response_file" \
        -w '%{http_code}' \
        -H 'Content-Type: application/json' \
        -d "$request_body" \
        "https://${host}:${port}${path}" || true
    )"
    if [ "$status" = "$expected_status" ] && grep -Fq "$expected_fragment" "$response_file"; then
      rm -f "$response_file"
      return 0
    fi

    sleep 2
    attempt=$((attempt + 1))
  done

  echo "Expected HTTP $expected_status from https://${host}:${port}${path} with response containing: $expected_fragment" >&2
  echo "Last status was ${status:-none}" >&2
  if [ -s "$response_file" ]; then
    echo "Last response body:" >&2
    cat "$response_file" >&2
  fi
  rm -f "$response_file"
  exit 1
}

for service in $expected_services; do
  assert_service_running "$service"
done

for service in $healthy_services; do
  wait_for_service_healthy "$service"
done

wait_for_status "http://127.0.0.1:${BACKEND_API_PORT:-8080}/internal/health" "200"
wait_for_status "http://127.0.0.1:${BACKEND_API_PORT:-8080}/internal/readiness" "200"
wait_for_host_status "${PUBLIC_ADMIN_DOMAIN}" "${PUBLIC_HTTPS_PORT:-443}" "/" "200"
wait_for_host_status "${PUBLIC_ADMIN_DOMAIN}" "${PUBLIC_HTTPS_PORT:-443}" "/config.js" "200"
wait_for_host_status "${PUBLIC_API_DOMAIN}" "${PUBLIC_HTTPS_PORT:-443}" "/api/v1/session/me" "401"
wait_for_host_json_response \
  "${PUBLIC_API_DOMAIN}" \
  "${PUBLIC_HTTPS_PORT:-443}" \
  "/api/v1/auth/magic-link/request" \
  "400" \
  '{"phone":"abc"}' \
  '"code":"validation_failed"'
wait_for_host_json_response \
  "${PUBLIC_API_DOMAIN}" \
  "${PUBLIC_HTTPS_PORT:-443}" \
  "/api/v1/whatsapp/webhooks/evolution" \
  "401" \
  '{}' \
  '"code":"authentication_failed"'
