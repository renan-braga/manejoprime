#!/usr/bin/env bash
# Hook: validates that git checkout -b / git switch -c includes a ticket ID.
# Valid ticket prefixes: BE, FM, FAW, AUTH, WA, INFRA, QA, DOC
# Expected pattern: <anything>/<TICKET-ID>-<description>
# Example: renan-braga/WA-013-fix-lob-transactional

input=$(cat)
cmd=$(python3 -c "import json,sys; print(json.load(sys.stdin).get('command',''))" 2>/dev/null <<< "$input")

if echo "$cmd" | grep -qE 'git (checkout -b|switch -c)'; then
  branch=$(echo "$cmd" | grep -oE '(checkout -b|switch -c) [^[:space:]]+' | awk '{print $NF}')

  if ! echo "$branch" | grep -qiE '(BE|FM|FAW|AUTH|WA|INFRA|QA|DOC)-[0-9]+'; then
    echo "Branch name must include a ticket ID."
    echo "Expected: renan-braga/<TICKET-ID>-<description>  (e.g. renan-braga/WA-013-fix-lob)"
    echo "Got: \"$branch\""
    exit 2
  fi
fi

exit 0
