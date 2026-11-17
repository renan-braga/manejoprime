#!/bin/sh
set -eu

append_profile() {
  next_profile="$1"
  current_profiles="$2"

  if [ -z "${current_profiles}" ]; then
    printf '%s' "${next_profile}"
    return
  fi

  case ",${current_profiles}," in
    *,"${next_profile}",*)
      printf '%s' "${current_profiles}"
      ;;
    *)
      printf '%s,%s' "${current_profiles}" "${next_profile}"
      ;;
  esac
}

runtime_role="${CURRALZAP_RUNTIME_ROLE:-api}"
active_profiles="${SPRING_PROFILES_ACTIVE:-}"
active_profiles="$(append_profile container "${active_profiles}")"

case "${runtime_role}" in
  api)
    active_profiles="$(append_profile api "${active_profiles}")"
    ;;
  worker)
    active_profiles="$(append_profile worker "${active_profiles}")"
    ;;
  *)
    echo "Unsupported CURRALZAP_RUNTIME_ROLE: ${runtime_role}" >&2
    exit 1
    ;;
esac

# shellcheck disable=SC2086
exec java ${JAVA_OPTS:-} -jar /app/app.jar "--spring.profiles.active=${active_profiles}" "$@"
