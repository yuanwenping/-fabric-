package main

import (
	"log"

	"github.com/hyperledger/fabric-contract-api-go/contractapi"
)

func main() {
	contract := new(TraceContract)
	chaincode, err := contractapi.NewChaincode(contract)
	if err != nil {
		log.Panicf("failed to create trace chaincode: %v", err)
	}
	if err := chaincode.Start(); err != nil {
		log.Panicf("failed to start trace chaincode: %v", err)
	}
}
