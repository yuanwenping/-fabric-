#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export ROOT
source "$ROOT/config/supplier.env"

mkdir -p "$ROOT/backend/run" "$ROOT/uploads"
PID_FILE="$ROOT/backend/run/backend.pid"
LOG_FILE="$ROOT/backend/run/backend.log"

if [[ -f "$PID_FILE" ]] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
    echo "Backend is already running: PID $(cat "$PID_FILE")"
    exit 0
fi

cd "$ROOT/backend"
nohup java -jar trace-backend-1.0.0.jar \
    --server.port="$SERVER_PORT" \
    >"$LOG_FILE" 2>&1 &
echo $! > "$PID_FILE"
echo "Backend started: PID $(cat "$PID_FILE"), log $LOG_FILE"
