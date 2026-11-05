#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export ROOT
source "$ROOT/config/supplier.env"

mkdir -p "$ROOT/backend/run"
PID_FILE="$ROOT/backend/run/frontend.pid"
LOG_FILE="$ROOT/backend/run/frontend.log"

if [[ -f "$PID_FILE" ]] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
    echo "Frontend is already running: PID $(cat "$PID_FILE")"
    exit 0
fi

nohup python3 "$ROOT/frontend/gateway.py" \
    --host 0.0.0.0 --port "$FRONTEND_PORT" \
    --static-dir "$ROOT/frontend/dist" \
    --api-target "http://127.0.0.1:$SERVER_PORT" \
    >"$LOG_FILE" 2>&1 &
echo $! > "$PID_FILE"
echo "Frontend started: PID $(cat "$PID_FILE"), log $LOG_FILE"
