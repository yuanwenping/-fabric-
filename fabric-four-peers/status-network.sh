#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"
echo "Host IP: $(hostname -I | awk '{print $1}')"
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' | \
    grep -E 'NAMES|orderer|peer0|trace4-cli' || true
echo
echo "Channel peers:"
docker exec trace4-cli env \
    CORE_PEER_TLS_ENABLED=true \
    CORE_PEER_LOCALMSPID=SupplierMSP \
    CORE_PEER_TLS_ROOTCERT_FILE=/work/organizations/peerOrganizations/supplier.example.com/peers/peer0.supplier.example.com/tls/ca.crt \
    CORE_PEER_MSPCONFIGPATH=/work/organizations/peerOrganizations/supplier.example.com/users/Admin@supplier.example.com/msp \
    CORE_PEER_ADDRESS=peer0.supplier.example.com:7051 \
    peer channel list || true
