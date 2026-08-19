#!/bin/bash

set -euo pipefail

cd "$(dirname "$0")/.."

echo "Building ecommerce monolith..."
./gradlew clean bootJar

echo "Build complete. Run: docker-compose up --build"
