#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
"$ROOT/scripts/stop-all.sh" || true
if [[ -n "${SUDO_PASSWORD:-}" ]]; then
    echo "$SUDO_PASSWORD" | sudo -S cp "$ROOT/systemd/"*.service /etc/systemd/system/
    echo "$SUDO_PASSWORD" | sudo -S systemctl daemon-reload
    echo "$SUDO_PASSWORD" | sudo -S systemctl enable supplier-trace-backend.service supplier-trace-frontend.service
    echo "$SUDO_PASSWORD" | sudo -S systemctl restart supplier-trace-backend.service supplier-trace-frontend.service
    echo "$SUDO_PASSWORD" | sudo -S systemctl status supplier-trace-backend.service --no-pager
    echo "$SUDO_PASSWORD" | sudo -S systemctl status supplier-trace-frontend.service --no-pager
else
    sudo cp "$ROOT/systemd/"*.service /etc/systemd/system/
    sudo systemctl daemon-reload
    sudo systemctl enable supplier-trace-backend.service supplier-trace-frontend.service
    sudo systemctl restart supplier-trace-backend.service supplier-trace-frontend.service
    sudo systemctl status supplier-trace-backend.service --no-pager
    sudo systemctl status supplier-trace-frontend.service --no-pager
fi
