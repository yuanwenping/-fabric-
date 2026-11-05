#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
"$ROOT/scripts/start-backend.sh"
sleep 8
"$ROOT/scripts/start-frontend.sh"
echo "Supplier node: http://192.168.116.145:5173"
