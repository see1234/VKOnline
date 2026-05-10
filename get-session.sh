#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

SESSION_FILE="${VK_SESSION_FILE:-$ROOT_DIR/vk-session.json}"

echo "Project: $ROOT_DIR"
echo "Session file: $SESSION_FILE"
echo
echo "A browser window will open. Log in to VK there, then return to this terminal and press Enter."
echo

VK_SESSION_FILE="$SESSION_FILE" ./gradlew run --args="session-login" --no-daemon --console=plain

echo
echo "Done. Copy this file to the server:"
echo "  $SESSION_FILE"
