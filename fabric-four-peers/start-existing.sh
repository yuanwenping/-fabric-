#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"
echo 123456 | sudo -S systemctl start docker
docker-compose -p trace4 -f docker/docker-compose.yaml up -d
echo "Fabric containers started."
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
