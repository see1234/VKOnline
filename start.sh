#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

if [[ $# -lt 1 ]]; then
  cat <<'EOF'
Usage:
  ./start.sh <vk_owner> [query] [count]

Examples:
  ./start.sh durov
  ./start.sh durov java 10
  ./start.sh wall-1 news 20

Optional env:
  VK_SESSION_FILE=/absolute/path/to/vk-session.json
EOF
  exit 1
fi

SESSION_FILE="${VK_SESSION_FILE:-$ROOT_DIR/vk-session.json}"
OWNER="$1"
QUERY="${2:-}"
COUNT="${3:-10}"

if [[ ! -f "$SESSION_FILE" ]]; then
  echo "Session file not found: $SESSION_FILE" >&2
  echo "First create it on a computer with GUI using ./get-session.sh and copy it here." >&2
  exit 1
fi

ARGS=("session-check" "$OWNER")
if [[ -n "$QUERY" ]]; then
  ARGS+=("$QUERY")
fi
if [[ -n "$COUNT" ]]; then
  ARGS+=("$COUNT")
fi

ARG_STRING=""
for arg in "${ARGS[@]}"; do
  if [[ -n "$ARG_STRING" ]]; then
    ARG_STRING+=" "
  fi
  ARG_STRING+="$arg"
done

VK_SESSION_FILE="$SESSION_FILE" ./gradlew run --args="$ARG_STRING" --no-daemon --console=plain
