#!/bin/bash

set -euo pipefail

echo "Checking ecommerce monolith..."
curl -f http://localhost:8080/actuator/health
