#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

echo "Installing system packages for Ubuntu 22.04..."
sudo apt update
sudo apt install -y \
  openjdk-21-jdk \
  unzip \
  curl \
  ca-certificates \
  fonts-liberation \
  libasound2 \
  libatk-bridge2.0-0 \
  libatk1.0-0 \
  libatspi2.0-0 \
  libcups2 \
  libdrm2 \
  libgbm1 \
  libglib2.0-0 \
  libgtk-3-0 \
  libnspr4 \
  libnss3 \
  libxcomposite1 \
  libxdamage1 \
  libxfixes3 \
  libxkbcommon0 \
  libxrandr2 \
  libxshmfence1

echo
echo "Java version:"
java -version

echo
echo "Warming up Gradle and compiling project..."
./gradlew compileJava --no-daemon --console=plain

echo
echo "Setup complete."
echo "Next:"
echo "  1. Put vk-session.json into: $ROOT_DIR"
echo "  2. Run: ./start.sh durov"
