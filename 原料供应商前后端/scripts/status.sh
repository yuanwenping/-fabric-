#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
echo "Supplier IP: $(hostname -I | awk '{print $1}')"
for name in backend frontend; do
    FILE="$ROOT/backend/run/$name.pid"
    if [[ -f "$FILE" ]] && kill -0 "$(cat "$FILE")" 2>/dev/null; then
        echo "$name: RUNNING PID $(cat "$FILE")"
    else
        echo "$name: STOPPED"
    fi
done
echo "Backend health:"
wget -qO- http://127.0.0.1:8080/actuator/health || true
echo
