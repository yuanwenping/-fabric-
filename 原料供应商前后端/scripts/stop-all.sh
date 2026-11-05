#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
for name in frontend backend; do
    FILE="$ROOT/backend/run/$name.pid"
    if [[ -f "$FILE" ]]; then
        PID="$(cat "$FILE")"
        if kill -0 "$PID" 2>/dev/null; then
            kill "$PID"
            echo "Stopped $name: PID $PID"
        fi
        rm -f "$FILE"
    fi
done
