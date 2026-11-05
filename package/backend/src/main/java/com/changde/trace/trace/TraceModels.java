package com.changde.trace.trace;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class TraceModels {
    private TraceModels() {
    }

    public record BatchView(
            String id,
            String batchNo,
            String traceCode,
            String batchType,
            Long ownerOrgId,
            String ownerOrgName,
            Long custodianOrgId,
            String custodianOrgName,
            String productName,
            String spec,
            BigDecimal quantity,
            String unit,
            String currentStage,
            String status,
            LocalDate productionDate,
            LocalDate expiryDate,
            String lastEventId,
            String lastHash,
            String chainStatus,
            String txId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record EventView(
            String id,
            String batchId,
            String eventType,
            String stage,
            String resultStatus,
            Long issuerOrgId,
            String issuerOrgName,
            String operatorName,
            LocalDateTime businessTime,
            String locationCode,
            JsonNode processData,
            String contentHash,
            String evidenceRoot,
            String prevEventId,
            String prevHash,
            String chainStatus,
            String txId,
            String chainError,
            LocalDateTime createdAt,
            List<EvidenceView> evidence,
            String reviewStatus,
            String reviewerName,
            LocalDateTime reviewedAt,
            String reviewComment
    ) {
    }

    public record EvidenceView(
            String id,
            String originalName,
            String contentType,
            long fileSize,
            String sha256,
            LocalDateTime createdAt
    ) {
    }

    public record RelationView(
            String parentBatchId,
            String parentBatchNo,
            String childBatchId,
            String childBatchNo,
            String relationType,
            BigDecimal quantity
    ) {
    }

    public record TransferView(
            String id,
            Long fromOrgId,
            String fromOrgName,
            Long toOrgId,
            String toOrgName,
            Long carrierOrgId,
            String carrierOrgName,
            String status,
            LocalDateTime outboundAt,
            LocalDateTime receivedAt,
            String discrepancyCode
    ) {
    }

    public record InspectionView(
            String id,
            String inspectionType,
            String sampleNo,
            JsonNode indicators,
            String overallResult,
            String reportNo,
            String reportHash,
            LocalDateTime issuedAt,
            String decision,
            String remark
    ) {
    }

    public record BatchDetail(
            BatchView batch,
            List<EventView> events,
            List<RelationView> relations,
            List<TransferView> transfers,
            List<InspectionView> inspections
    ) {
    }

    public record PublicTrace(
            String traceCode,
            String productName,
            String spec,
            String quantity,
            String unit,
            String ownerOrgName,
            String currentStage,
            String status,
            LocalDate productionDate,
            LocalDate expiryDate,
            List<PublicEvent> events,
            List<PublicInspection> inspections,
            String chainStatus,
            String lastHash,
            String txId
    ) {
    }

    public record PublicEvent(
            String eventType,
            String stage,
            String issuerOrgName,
            LocalDateTime businessTime,
            String locationCode,
            JsonNode summary,
            String chainStatus,
            String txId
    ) {
    }

    public record PublicInspection(
            String inspectionType,
            String overallResult,
            String reportNo,
            LocalDateTime issuedAt,
            String decision
    ) {
    }

    public record ChainSubmission(String transactionId, Long blockNumber) {
    }
}
