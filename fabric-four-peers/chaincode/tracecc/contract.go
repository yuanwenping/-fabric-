package main

import (
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"strings"

	"github.com/hyperledger/fabric-contract-api-go/contractapi"
)

const (
	batchPrefix = "batch"
	eventPrefix = "event"
	indexPrefix = "event~batch"
)

type TraceContract struct {
	contractapi.Contract
}

type Batch struct {
	BatchID           string `json:"batchId"`
	BatchNo           string `json:"batchNo"`
	TraceCode         string `json:"traceCode"`
	BatchType         string `json:"batchType"`
	OwnerOrgID        string `json:"ownerOrgId"`
	CustodianOrgID    string `json:"custodianOrgId"`
	ProductName       string `json:"productName"`
	Spec              string `json:"spec"`
	Quantity          string `json:"quantity"`
	Unit              string `json:"unit"`
	Stage             string `json:"stage"`
	Status            string `json:"status"`
	ProductionDate    string `json:"productionDate"`
	ExpiryDate        string `json:"expiryDate"`
	ParentBatchIDs    []string `json:"parentBatchIds"`
	LastEventID       string `json:"lastEventId"`
	LastHash          string `json:"lastHash"`
	CreatedAt         string `json:"createdAt"`
}

type TraceEvent struct {
	EventID        string `json:"eventId"`
	BatchID        string `json:"batchId"`
	EventType      string `json:"eventType"`
	Stage          string `json:"stage"`
	Status         string `json:"status"`
	IssuerOrgID    string `json:"issuerOrgId"`
	OperatorUserID string `json:"operatorUserId"`
	BusinessTime   string `json:"businessTime"`
	LocationCode   string `json:"locationCode"`
	DataJSON       string `json:"dataJson"`
	PrevEventID    string `json:"prevEventId"`
	PrevHash       string `json:"prevHash"`
	ContentHash    string `json:"contentHash"`
	EvidenceRoot   string `json:"evidenceRoot"`
}

type TraceView struct {
	Batch  Batch        `json:"batch"`
	Events []TraceEvent `json:"events"`
}

func (c *TraceContract) CreateBatch(ctx contractapi.TransactionContextInterface, payload string) (string, error) {
	mspID, err := ctx.GetClientIdentity().GetMSPID()
	if err != nil {
		return "", fmt.Errorf("read client identity: %w", err)
	}
	if !isBusinessMSP(mspID) {
		return "", fmt.Errorf("MSP %s may not create trace batches", mspID)
	}

	var batch Batch
	if err := json.Unmarshal([]byte(payload), &batch); err != nil {
		return "", fmt.Errorf("decode batch: %w", err)
	}
	if strings.TrimSpace(batch.BatchID) == "" {
		return "", fmt.Errorf("batchId is required")
	}
	if batch.CreatedAt == "" {
		batch.CreatedAt = ""
	}
	key, err := ctx.GetStub().CreateCompositeKey(batchPrefix, []string{batch.BatchID})
	if err != nil {
		return "", err
	}
	existing, err := ctx.GetStub().GetState(key)
	if err != nil {
		return "", err
	}
	if len(existing) > 0 {
		return string(existing), nil
	}

	if batch.LastHash == "" {
		hash := sha256.Sum256([]byte(payload))
		batch.LastHash = hex.EncodeToString(hash[:])
	}
	bytes, err := json.Marshal(batch)
	if err != nil {
		return "", err
	}
	if err := ctx.GetStub().PutState(key, bytes); err != nil {
		return "", err
	}
	return string(bytes), nil
}

func (c *TraceContract) AppendEvent(ctx contractapi.TransactionContextInterface, payload string) (string, error) {
	var event TraceEvent
	if err := json.Unmarshal([]byte(payload), &event); err != nil {
		return "", fmt.Errorf("decode event: %w", err)
	}
	if event.EventID == "" || event.BatchID == "" || event.ContentHash == "" {
		return "", fmt.Errorf("eventId, batchId and contentHash are required")
	}

	mspID, err := ctx.GetClientIdentity().GetMSPID()
	if err != nil {
		return "", fmt.Errorf("read client identity: %w", err)
	}
	regulatory := isRegulatoryEvent(event.EventType)
	if regulatory && mspID != "Org2MSP" {
		return "", fmt.Errorf("regulatory event %s requires Org2MSP", event.EventType)
	}
	if !regulatory && !isBusinessMSP(mspID) {
		return "", fmt.Errorf("business event %s is not allowed for MSP %s", event.EventType, mspID)
	}

	batchKey, err := ctx.GetStub().CreateCompositeKey(batchPrefix, []string{event.BatchID})
	if err != nil {
		return "", err
	}
	batchBytes, err := ctx.GetStub().GetState(batchKey)
	if err != nil {
		return "", err
	}
	if len(batchBytes) == 0 {
		return "", fmt.Errorf("batch %s does not exist", event.BatchID)
	}
	var batch Batch
	if err := json.Unmarshal(batchBytes, &batch); err != nil {
		return "", err
	}
	if event.PrevHash != batch.LastHash {
		return "", fmt.Errorf("previous hash mismatch for batch %s", event.BatchID)
	}
	if event.PrevEventID != batch.LastEventID {
		return "", fmt.Errorf("previous event mismatch for batch %s", event.BatchID)
	}

	eventKey, err := ctx.GetStub().CreateCompositeKey(eventPrefix, []string{event.EventID})
	if err != nil {
		return "", err
	}
	existing, err := ctx.GetStub().GetState(eventKey)
	if err != nil {
		return "", err
	}
	if len(existing) > 0 {
		var existingEvent TraceEvent
		if err := json.Unmarshal(existing, &existingEvent); err != nil {
			return "", err
		}
		if strings.EqualFold(existingEvent.ContentHash, event.ContentHash) {
			return string(existing), nil
		}
		return "", fmt.Errorf("event %s already exists with a different hash", event.EventID)
	}
	indexKey, err := ctx.GetStub().CreateCompositeKey(indexPrefix, []string{event.BatchID, event.EventID})
	if err != nil {
		return "", err
	}

	eventBytes, err := json.Marshal(event)
	if err != nil {
		return "", err
	}
	if err := ctx.GetStub().PutState(eventKey, eventBytes); err != nil {
		return "", err
	}
	if err := ctx.GetStub().PutState(indexKey, []byte{0}); err != nil {
		return "", err
	}

	batch.LastEventID = event.EventID
	batch.LastHash = event.ContentHash
	if event.Stage != "" {
		batch.Stage = event.Stage
	}
	if event.Status != "" {
		batch.Status = event.Status
	}
	updatedBatch, err := json.Marshal(batch)
	if err != nil {
		return "", err
	}
	if err := ctx.GetStub().PutState(batchKey, updatedBatch); err != nil {
		return "", err
	}
	return string(eventBytes), nil
}

func (c *TraceContract) GetBatch(ctx contractapi.TransactionContextInterface, batchID string) (string, error) {
	key, err := ctx.GetStub().CreateCompositeKey(batchPrefix, []string{batchID})
	if err != nil {
		return "", err
	}
	value, err := ctx.GetStub().GetState(key)
	if err != nil {
		return "", err
	}
	if len(value) == 0 {
		return "", fmt.Errorf("batch %s does not exist", batchID)
	}
	return string(value), nil
}

func (c *TraceContract) GetEvent(ctx contractapi.TransactionContextInterface, eventID string) (string, error) {
	key, err := ctx.GetStub().CreateCompositeKey(eventPrefix, []string{eventID})
	if err != nil {
		return "", err
	}
	value, err := ctx.GetStub().GetState(key)
	if err != nil {
		return "", err
	}
	if len(value) == 0 {
		return "", fmt.Errorf("event %s does not exist", eventID)
	}
	return string(value), nil
}

func (c *TraceContract) QueryEvents(ctx contractapi.TransactionContextInterface, batchID string) ([]TraceEvent, error) {
	iterator, err := ctx.GetStub().GetStateByPartialCompositeKey(indexPrefix, []string{batchID})
	if err != nil {
		return nil, err
	}
	defer iterator.Close()

	events := make([]TraceEvent, 0)
	for iterator.HasNext() {
		item, err := iterator.Next()
		if err != nil {
			return nil, err
		}
		_, parts, err := ctx.GetStub().SplitCompositeKey(item.Key)
		if err != nil {
			return nil, err
		}
		if len(parts) != 2 {
			continue
		}
		eventKey, err := ctx.GetStub().CreateCompositeKey(eventPrefix, []string{parts[1]})
		if err != nil {
			return nil, err
		}
		eventBytes, err := ctx.GetStub().GetState(eventKey)
		if err != nil {
			return nil, err
		}
		var event TraceEvent
		if err := json.Unmarshal(eventBytes, &event); err != nil {
			return nil, err
		}
		events = append(events, event)
	}
	return events, nil
}

func (c *TraceContract) GetTrace(ctx contractapi.TransactionContextInterface, batchID string) (string, error) {
	batchPayload, err := c.GetBatch(ctx, batchID)
	if err != nil {
		return "", err
	}
	events, err := c.QueryEvents(ctx, batchID)
	if err != nil {
		return "", err
	}
	var batch Batch
	if err := json.Unmarshal([]byte(batchPayload), &batch); err != nil {
		return "", err
	}
	view := TraceView{Batch: batch, Events: events}
	bytes, err := json.Marshal(view)
	if err != nil {
		return "", err
	}
	return string(bytes), nil
}

func (c *TraceContract) VerifyEvent(ctx contractapi.TransactionContextInterface, eventID string, expectedHash string) (bool, error) {
	payload, err := c.GetEvent(ctx, eventID)
	if err != nil {
		return false, err
	}
	var event TraceEvent
	if err := json.Unmarshal([]byte(payload), &event); err != nil {
		return false, err
	}
	return strings.EqualFold(event.ContentHash, expectedHash), nil
}

func isRegulatoryEvent(eventType string) bool {
	switch eventType {
	case "INSPECTION", "FREEZE", "RELEASE", "RECALL":
		return true
	default:
		return false
	}
}

func isBusinessMSP(mspID string) bool {
	switch mspID {
	case "Org1MSP", "SupplierMSP", "ProcessorMSP", "LogisticsMSP", "RetailerMSP":
		return true
	default:
		return false
	}
}
