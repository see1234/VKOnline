#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

SESSION_FILE="${VK_SESSION_FILE:-$ROOT_DIR/vk-session.json}"
INTERVAL="${1:-240}"
TARGET="${2:-feed}"

if [[ ! -f "$SESSION_FILE" ]]; then
  echo "Session file not found: $SESSION_FILE" >&2
  echo "First create it on a computer with GUI using ./get-session.sh and copy it here." >&2
  exit 1
fi

VK_SESSION_FILE="$SESSION_FILE" ./gradlew run --args="online-heartbeat $INTERVAL $TARGET" --no-daemon --console=plain
