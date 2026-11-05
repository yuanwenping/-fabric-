#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
FABRIC_BIN="/home/yuan/fabric-samples/bin"
CHANNEL="tracechannel"
CHAINCODE="tracecc"
VERSION="1.0"
SEQUENCE="1"
IMAGE_TAG="2.2.15"

export PATH="$FABRIC_BIN:$PATH"
export FABRIC_CFG_PATH="$ROOT"
export IMAGE_TAG

cd "$ROOT"

echo "[1/8] Stop old demo peers while preserving old volumes"
docker stop orderer.example.com peer0.org1.example.com peer0.org2.example.com >/dev/null 2>&1 || true

echo "[2/8] Generate MSP material"
if [[ ! -d organizations/peerOrganizations/supplier.example.com ]]; then
    cryptogen generate --config="$ROOT/crypto-config.yaml" --output="$ROOT/organizations"
fi

echo "[3/8] Generate genesis block and channel transaction"
mkdir -p channel-artifacts
configtxgen -profile FourOrgOrdererGenesis -channelID system-channel \
    -outputBlock "$ROOT/channel-artifacts/genesis.block"
configtxgen -profile FourOrgChannel -outputCreateChannelTx \
    "$ROOT/channel-artifacts/${CHANNEL}.tx" -channelID "$CHANNEL"

echo "[4/8] Start four peers and one orderer"
docker-compose -p trace4 -f docker/docker-compose.yaml up -d
sleep 5

ORG_BASE="$ROOT/organizations/peerOrganizations"
ORDERER_CA="$ROOT/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem"

peer_env() {
    local org="$1" port="$2"
    export CORE_PEER_TLS_ENABLED=true
    export CORE_PEER_LOCALMSPID="${org}MSP"
    export CORE_PEER_TLS_ROOTCERT_FILE="$ORG_BASE/${org,,}.example.com/peers/peer0.${org,,}.example.com/tls/ca.crt"
    export CORE_PEER_MSPCONFIGPATH="$ORG_BASE/${org,,}.example.com/users/Admin@${org,,}.example.com/msp"
    export CORE_PEER_ADDRESS="peer0.${org,,}.example.com:${port}"
}

echo "[5/8] Create channel and join four peers"
peer_env Supplier 7051
docker exec trace4-cli env \
    CORE_PEER_TLS_ENABLED=true \
    CORE_PEER_LOCALMSPID=SupplierMSP \
    CORE_PEER_TLS_ROOTCERT_FILE="/work/organizations/peerOrganizations/supplier.example.com/peers/peer0.supplier.example.com/tls/ca.crt" \
    CORE_PEER_MSPCONFIGPATH="/work/organizations/peerOrganizations/supplier.example.com/users/Admin@supplier.example.com/msp" \
    CORE_PEER_ADDRESS=peer0.supplier.example.com:7051 \
    peer channel create -o orderer.example.com:7050 \
    --ordererTLSHostnameOverride orderer.example.com \
    -c "$CHANNEL" -f "/work/channel-artifacts/${CHANNEL}.tx" \
    --outputBlock "/work/channel-artifacts/${CHANNEL}.block" \
    --tls --cafile /work/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem

for spec in "Supplier 7051" "Processor 8051" "Logistics 9051" "Retailer 10051"; do
    set -- $spec
    org="$1"
    port="$2"
    lower="${org,,}"
    docker exec trace4-cli env \
        CORE_PEER_TLS_ENABLED=true \
        CORE_PEER_LOCALMSPID="${org}MSP" \
        CORE_PEER_TLS_ROOTCERT_FILE="/work/organizations/peerOrganizations/${lower}.example.com/peers/peer0.${lower}.example.com/tls/ca.crt" \
        CORE_PEER_MSPCONFIGPATH="/work/organizations/peerOrganizations/${lower}.example.com/users/Admin@${lower}.example.com/msp" \
        CORE_PEER_ADDRESS="peer0.${lower}.example.com:${port}" \
        peer channel join -b "/work/channel-artifacts/${CHANNEL}.block"
done

echo "[6/8] Package and install chaincode on four peers"
if [[ ! -d "$ROOT/chaincode/tracecc/vendor" ]]; then
    (cd "$ROOT/chaincode/tracecc" && go mod vendor)
fi

docker exec trace4-cli peer lifecycle chaincode package \
    "/work/chaincode/${CHAINCODE}.tar.gz" \
    --path /work/chaincode/tracecc --lang golang --label "${CHAINCODE}_${VERSION}"

PACKAGE_ID=""
for spec in "Supplier 7051" "Processor 8051" "Logistics 9051" "Retailer 10051"; do
    set -- $spec
    org="$1"
    port="$2"
    lower="${org,,}"
    OUT=$(docker exec trace4-cli env \
        CORE_PEER_TLS_ENABLED=true \
        CORE_PEER_LOCALMSPID="${org}MSP" \
        CORE_PEER_TLS_ROOTCERT_FILE="/work/organizations/peerOrganizations/${lower}.example.com/peers/peer0.${lower}.example.com/tls/ca.crt" \
        CORE_PEER_MSPCONFIGPATH="/work/organizations/peerOrganizations/${lower}.example.com/users/Admin@${lower}.example.com/msp" \
        CORE_PEER_ADDRESS="peer0.${lower}.example.com:${port}" \
        peer lifecycle chaincode install "/work/chaincode/${CHAINCODE}.tar.gz")
    PKG=$(printf '%s\n' "$OUT" | awk '/Chaincode code package identifier:/ {print $5}' | tail -1)
    if [[ -z "$PKG" ]]; then
        PKG=$(docker exec trace4-cli env \
            CORE_PEER_TLS_ENABLED=true \
            CORE_PEER_LOCALMSPID="${org}MSP" \
            CORE_PEER_TLS_ROOTCERT_FILE="/work/organizations/peerOrganizations/${lower}.example.com/peers/peer0.${lower}.example.com/tls/ca.crt" \
            CORE_PEER_MSPCONFIGPATH="/work/organizations/peerOrganizations/${lower}.example.com/users/Admin@${lower}.example.com/msp" \
            CORE_PEER_ADDRESS="peer0.${lower}.example.com:${port}" \
            peer lifecycle chaincode queryinstalled | awk -F'[, ]+' '/Package ID:/ {print $3}' | tail -1)
    fi
    PACKAGE_ID="$PKG"
done

echo "[7/8] Approve chaincode for all organizations"
ENDORSE_POLICY="OR('SupplierMSP.peer','ProcessorMSP.peer','LogisticsMSP.peer','RetailerMSP.peer')"
for spec in "Supplier 7051" "Processor 8051" "Logistics 9051" "Retailer 10051"; do
    set -- $spec
    org="$1"
    port="$2"
    lower="${org,,}"
    docker exec trace4-cli env \
        CORE_PEER_TLS_ENABLED=true \
        CORE_PEER_LOCALMSPID="${org}MSP" \
        CORE_PEER_TLS_ROOTCERT_FILE="/work/organizations/peerOrganizations/${lower}.example.com/peers/peer0.${lower}.example.com/tls/ca.crt" \
        CORE_PEER_MSPCONFIGPATH="/work/organizations/peerOrganizations/${lower}.example.com/users/Admin@${lower}.example.com/msp" \
        CORE_PEER_ADDRESS="peer0.${lower}.example.com:${port}" \
        peer lifecycle chaincode approveformyorg \
        -o orderer.example.com:7050 \
        --ordererTLSHostnameOverride orderer.example.com \
        --channelID "$CHANNEL" --name "$CHAINCODE" \
        --version "$VERSION" --package-id "$PACKAGE_ID" --sequence "$SEQUENCE" \
        --tls --cafile /work/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem \
        --signature-policy "$ENDORSE_POLICY"
done

echo "[8/8] Commit chaincode"
peer_env Supplier 7051
docker exec trace4-cli env \
    CORE_PEER_TLS_ENABLED=true \
    CORE_PEER_LOCALMSPID=SupplierMSP \
    CORE_PEER_TLS_ROOTCERT_FILE="/work/organizations/peerOrganizations/supplier.example.com/peers/peer0.supplier.example.com/tls/ca.crt" \
    CORE_PEER_MSPCONFIGPATH="/work/organizations/peerOrganizations/supplier.example.com/users/Admin@supplier.example.com/msp" \
    CORE_PEER_ADDRESS=peer0.supplier.example.com:7051 \
    peer lifecycle chaincode commit \
    -o orderer.example.com:7050 \
    --ordererTLSHostnameOverride orderer.example.com \
    --channelID "$CHANNEL" --name "$CHAINCODE" \
    --version "$VERSION" --sequence "$SEQUENCE" \
    --tls --cafile /work/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem \
    --signature-policy "$ENDORSE_POLICY" \
    --peerAddresses peer0.supplier.example.com:7051 \
    --tlsRootCertFiles /work/organizations/peerOrganizations/supplier.example.com/peers/peer0.supplier.example.com/tls/ca.crt \
    --peerAddresses peer0.processor.example.com:8051 \
    --tlsRootCertFiles /work/organizations/peerOrganizations/processor.example.com/peers/peer0.processor.example.com/tls/ca.crt \
    --peerAddresses peer0.logistics.example.com:9051 \
    --tlsRootCertFiles /work/organizations/peerOrganizations/logistics.example.com/peers/peer0.logistics.example.com/tls/ca.crt \
    --peerAddresses peer0.retailer.example.com:10051 \
    --tlsRootCertFiles /work/organizations/peerOrganizations/retailer.example.com/peers/peer0.retailer.example.com/tls/ca.crt

echo "Four-organization Fabric network is ready."
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
