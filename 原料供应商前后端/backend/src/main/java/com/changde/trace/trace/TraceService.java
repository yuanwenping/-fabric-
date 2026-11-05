package com.changde.trace.trace;

import com.changde.trace.api.ApiException;
import com.changde.trace.api.ApiModels;
import com.changde.trace.security.CurrentUser;
import com.changde.trace.security.SecuritySupport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;

@Service
public class TraceService {
    private static final DateTimeFormatter NUMBER_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final ObjectMapper canonicalMapper;
    private final SecuritySupport security;
    private final ChainService chain;
    private final TransactionTemplate transactions;

    public TraceService(
            JdbcTemplate jdbc,
            ObjectMapper objectMapper,
            SecuritySupport security,
            ChainService chain,
            TransactionTemplate transactions,
            @Value("${app.jwt-secret}") String ignoredForSignatureVersion
    ) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.canonicalMapper = objectMapper.copy()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        this.security = security;
        this.chain = chain;
        this.transactions = transactions;
    }

    public ApiModels.PageResponse<TraceModels.BatchView> listBatches(
            String keyword,
            String status,
            int page,
            int size
    ) {
        CurrentUser user = security.currentUser();
        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(1, size));
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (!user.isAdmin() && !user.isRegulator()) {
            where.append("""
                     AND EXISTS (
                       SELECT 1 FROM batch_party p
                       WHERE p.batch_id = b.id AND p.org_id = ?
                     )
                    """);
            params.add(user.orgId());
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (b.batch_no LIKE ? OR b.trace_code LIKE ? OR b.product_name LIKE ?)");
            String pattern = "%" + keyword.trim() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND b.status = ?");
            params.add(status.trim());
        }
        String countSql = "SELECT COUNT(*) FROM trace_batch b" + where;
        Long total = jdbc.queryForObject(countSql, Long.class, params.toArray());
        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(safeSize);
        pageParams.add((safePage - 1) * safeSize);
        List<TraceModels.BatchView> items = jdbc.query("""
                SELECT b.*, o.org_name AS owner_org_name, c.org_name AS custodian_org_name
                FROM trace_batch b
                JOIN organization o ON o.id = b.owner_org_id
                JOIN organization c ON c.id = b.custodian_org_id
                """ + where + " ORDER BY b.created_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> mapBatch(rs), pageParams.toArray());
        return new ApiModels.PageResponse<>(items, total == null ? 0 : total, safePage, safeSize);
    }

    public TraceModels.BatchDetail batchDetail(String batchId) {
        CurrentUser user = security.currentUser();
        TraceModels.BatchView batch = loadBatch(batchId);
        requireBatchAccess(batchId, user);
        return new TraceModels.BatchDetail(
                batch,
                loadEvents(batchId),
                loadRelations(batchId),
                loadTransfers(batchId),
                loadInspections(batchId)
        );
    }

    public TraceModels.BatchView createBatch(ApiModels.BatchCreateRequest request) {
        CurrentUser user = security.currentUser();
        if (!"SUPPLIER".equals(user.orgType())) {
            throw ApiException.forbidden("当前节点只能创建原料供应商批次");
        }
        if (!"RAW_DUCK".equals(request.batchType())) {
            throw ApiException.badRequest("原料供应商只能创建 RAW_DUCK 原料批次");
        }
        if (!user.hasRole("SUPPLIER_OPERATOR")) {
            throw ApiException.forbidden("当前岗位不能创建批次");
        }

        if (request.parentBatchIds() != null) {
            for (String parentId : new LinkedHashSet<>(request.parentBatchIds())) {
                TraceModels.BatchView parent = loadBatch(parentId);
                requireBatchAccess(parentId, user);
                if (!"RAW_DUCK".equals(parent.batchType())) {
                    throw ApiException.badRequest("原料供应商不能以该批次创建子批次");
                }
            }
        }

        String batchId = newId("B");
        String batchNo = batchNo(request.batchType());
        String traceCode = "FINISHED".equals(request.batchType()) ? newTraceCode() : null;
        String initialStage = "RAW_DUCK".equals(request.batchType()) ? "SOURCE" : "CREATED";
        Map<String, Object> batchPayload = new TreeMap<>();
        batchPayload.put("batchId", batchId);
        batchPayload.put("batchNo", batchNo);
        batchPayload.put("traceCode", valueOrEmpty(traceCode));
        batchPayload.put("batchType", request.batchType());
        batchPayload.put("ownerOrgId", String.valueOf(user.orgId()));
        batchPayload.put("custodianOrgId", String.valueOf(user.orgId()));
        batchPayload.put("productName", valueOrEmpty(request.productName()));
        batchPayload.put("spec", valueOrEmpty(request.spec()));
        batchPayload.put("quantity", request.quantity().toPlainString());
        batchPayload.put("unit", request.unit());
        batchPayload.put("stage", initialStage);
        batchPayload.put("status", "ACTIVE");
        batchPayload.put("productionDate", Objects.toString(request.productionDate(), ""));
        batchPayload.put("expiryDate", Objects.toString(request.expiryDate(), ""));
        batchPayload.put("parentBatchIds", request.parentBatchIds() == null ? List.of() : request.parentBatchIds());
        batchPayload.put("lastEventId", "");
        batchPayload.put("createdAt", LocalDateTime.now().toString());
        String batchHash = sha256(canonicalJson(batchPayload));
        batchPayload.put("lastHash", batchHash);

        jdbc.update("""
                INSERT INTO trace_batch (
                    id, batch_no, trace_code, batch_type, owner_org_id, custodian_org_id,
                    product_name, spec, quantity, unit, current_stage, status,
                    production_date, expiry_date, last_hash, chain_status, created_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?, ?, 'PENDING', ?)
                """,
                batchId, batchNo, traceCode, request.batchType(), user.orgId(), user.orgId(),
                request.productName(), request.spec(), request.quantity(), request.unit(),
                initialStage, request.productionDate(), request.expiryDate(), batchHash, user.id());
        jdbc.update("INSERT INTO batch_party (batch_id, org_id, party_type) VALUES (?, ?, 'OWNER')",
                batchId, user.orgId());

        if (request.parentBatchIds() != null) {
            for (String parentId : new LinkedHashSet<>(request.parentBatchIds())) {
                jdbc.update("""
                        INSERT IGNORE INTO batch_relation
                            (parent_batch_id, child_batch_id, relation_type, quantity)
                        VALUES (?, ?, ?, ?)
                        """, parentId, batchId,
                        request.relationType() == null ? "MERGE" : request.relationType(),
                        request.quantity());
            }
        }

        try {
            String mspId = jdbc.queryForObject(
                    "SELECT msp_id FROM organization WHERE id=?",
                    String.class,
                    user.orgId());
            TraceModels.ChainSubmission submission = chain.submit(
                    mspId, "CreateBatch", canonicalJson(batchPayload));
            jdbc.update("""
                    UPDATE trace_batch SET chain_status = 'COMMITTED', tx_id = ? WHERE id = ?
                    """, submission.transactionId(), batchId);
            audit(user, "CREATE_BATCH", "TRACE_BATCH", batchId, batchPayload);
        } catch (Exception ex) {
            jdbc.update("UPDATE trace_batch SET chain_status = 'FAILED' WHERE id = ?", batchId);
            throw ApiException.chainError("批次已保存但上链失败，可稍后重试：" + rootMessage(ex));
        }
        return loadBatch(batchId);
    }

    public TraceModels.EventView appendEvent(String batchId, ApiModels.EventRequest request) {
        CurrentUser user = security.currentUser();
        TraceModels.BatchView batch = loadBatch(batchId);
        requireBatchAccess(batchId, user);
        BusinessRules.requireEventPermission(user, request.eventType());
        BusinessRules.validateEventContent(request.eventType(), request.processData());
        if (!"DISPATCH".equals(request.eventType())
                && (request.targetOrgId() != null || request.carrierOrgId() != null)) {
            throw ApiException.badRequest("只有发货交接可以填写收货企业和承运企业");
        }
        if (request.correctionOf() != null && !request.correctionOf().isBlank()) {
            throw ApiException.badRequest("业务更正功能暂未开放");
        }
        boolean receivingActor = "RECEIVE".equals(request.eventType())
                && hasPendingReceipt(batchId, user.orgId());
        boolean carrierActor = Set.of("TRANSPORT", "DELIVER").contains(request.eventType())
                && hasAssignedCarrierTask(batchId, user.orgId());
        if (!user.isRegulator()
                && !Objects.equals(batch.custodianOrgId(), user.orgId())
                && !receivingActor
                && !carrierActor) {
            throw ApiException.forbidden("当前批次不在本企业保管范围内");
        }

        List<Map<String, Object>> existing = jdbc.queryForList(
                "SELECT id FROM trace_event WHERE request_id = ?", request.requestId());
        if (!existing.isEmpty()) {
            return loadEvent(String.valueOf(existing.get(0).get("id")));
        }

        if (!BusinessRules.REGULATORY_EVENTS.contains(request.eventType())) {
            BusinessRules.validateStage(request.eventType(), batch.currentStage());
        }
        if ("INSPECTION".equals(request.eventType()) && !user.isRegulator()) {
            throw ApiException.forbidden("只有监管部门可以提交检验结论");
        }

        LocalDateTime businessTime =
                request.businessTime() == null ? LocalDateTime.now() : request.businessTime();
        String eventId = newId("E");
        String stage = BusinessRules.REGULATORY_EVENTS.contains(request.eventType())
                ? batch.currentStage()
                : BusinessRules.stageFor(request.eventType());
        if (stage == null) {
            stage = batch.currentStage();
        }
        final String eventStage = stage;
        Object inspectionResult = request.processData() == null
                ? null
                : request.processData().get("overallResult");
        String resultStatus =
                BusinessRules.statusFor(request.eventType());
        if ("INSPECTION".equals(request.eventType())
                && !"FAIL".equalsIgnoreCase(String.valueOf(inspectionResult))) {
            resultStatus = batch.status();
        }
        if (Set.of("QUALITY_CHECK", "BREEDING_RECORD").contains(request.eventType())) {
            resultStatus = "FAIL".equalsIgnoreCase(String.valueOf(inspectionResult))
                    ? "FROZEN" : batch.status();
        }
        final String eventResultStatus = resultStatus;

        List<String> evidenceIds = request.evidenceIds() == null
                ? List.of()
                : request.evidenceIds().stream().filter(Objects::nonNull).distinct().toList();
        String evidenceRoot = computeEvidenceRoot(batchId, evidenceIds);

        Map<String, Object> content = new TreeMap<>();
        content.put("eventId", eventId);
        content.put("batchId", batchId);
        content.put("eventType", request.eventType());
        content.put("stage", stage);
        content.put("resultStatus", resultStatus);
        content.put("issuerOrgId", String.valueOf(user.orgId()));
        content.put("operatorUserId", String.valueOf(user.id()));
        content.put("businessTime", businessTime.toString());
        content.put("locationCode", valueOrEmpty(request.locationCode()));
        content.put("processData", request.processData() == null ? Map.of() : request.processData());
        content.put("evidenceRoot", valueOrEmpty(evidenceRoot));
        content.put("prevEventId", valueOrEmpty(batch.lastEventId()));
        content.put("prevHash", valueOrEmpty(batch.lastHash()));
        content.put("targetOrgId", Objects.toString(request.targetOrgId(), ""));
        content.put("carrierOrgId", Objects.toString(request.carrierOrgId(), ""));
        content.put("discrepancyCode", valueOrEmpty(request.discrepancyCode()));
        String canonical = canonicalJson(content);
        String contentHash = sha256(canonical + "|" + valueOrEmpty(evidenceRoot));
        String payloadJson = canonical;

        validateTransferRequest(batch, user, request);

        jdbc.update("""
                INSERT INTO trace_event (
                    id, batch_id, event_type, stage, result_status, issuer_org_id,
                    operator_user_id, business_time, location_code, payload_json,
                    content_hash, evidence_root, prev_event_id, prev_hash, request_id,
                    chain_status, correction_of
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING_REVIEW', ?)
                """,
                eventId, batchId, request.eventType(), stage, resultStatus, user.orgId(),
                user.id(), Timestamp.valueOf(businessTime), request.locationCode(), payloadJson,
                contentHash, evidenceRoot, batch.lastEventId(), batch.lastHash(), request.requestId(),
                request.correctionOf());
        linkEvidence(batchId, eventId, evidenceIds);
        String submittedRole = user.roles().stream()
                .filter(role -> !"ORG_MANAGER".equals(role))
                .findFirst()
                .orElse("ORG_MANAGER");
        jdbc.update("""
                INSERT INTO event_review (event_id, submitted_role_code, review_status)
                VALUES (?, ?, 'PENDING_REVIEW')
                """, eventId, submittedRole);
        audit(user, "SUBMIT_EVENT_REVIEW", "TRACE_EVENT", eventId, content);
        return loadEvent(eventId);
    }

    public List<Map<String, Object>> pendingReviews() {
        CurrentUser user = security.currentUser();
        requireOrganizationManager(user);
        return jdbc.queryForList("""
                SELECT e.id AS eventId, e.batch_id AS batchId, b.batch_no AS batchNo,
                       b.product_name AS productName, e.event_type AS eventType,
                       e.stage, e.result_status AS resultStatus, e.business_time AS businessTime,
                       e.location_code AS locationCode, e.payload_json AS payloadJson,
                       e.content_hash AS contentHash, e.evidence_root AS evidenceRoot,
                       u.display_name AS submittedByName, u.username AS submittedByPhone,
                       er.submitted_role_code AS submittedRole, er.created_at AS submittedAt
                FROM trace_event e
                JOIN event_review er ON er.event_id=e.id
                JOIN trace_batch b ON b.id=e.batch_id
                JOIN sys_user u ON u.id=e.operator_user_id
                WHERE e.issuer_org_id=? AND er.review_status='PENDING_REVIEW'
                ORDER BY er.created_at ASC
                """, user.orgId());
    }

    public List<Map<String, Object>> mySubmissions() {
        CurrentUser user = security.currentUser();
        return jdbc.queryForList("""
                SELECT e.id AS eventId, e.batch_id AS batchId, b.batch_no AS batchNo,
                       e.event_type AS eventType, e.stage, e.chain_status AS chainStatus,
                       e.created_at AS createdAt, er.review_status AS reviewStatus,
                       er.review_comment AS reviewComment, er.reviewed_at AS reviewedAt
                FROM trace_event e
                LEFT JOIN event_review er ON er.event_id=e.id
                JOIN trace_batch b ON b.id=e.batch_id
                WHERE e.operator_user_id=?
                ORDER BY e.created_at DESC
                LIMIT 100
                """, user.id());
    }

    public TraceModels.EventView approveEvent(String eventId, String comment) {
        CurrentUser reviewer = security.currentUser();
        Map<String, Object> row = loadReviewableEvent(eventId, reviewer);
        return submitApprovedEvent(row, reviewer, comment, true);
    }

    public TraceModels.EventView rejectEvent(String eventId, String comment) {
        CurrentUser reviewer = security.currentUser();
        Map<String, Object> row = loadReviewableEvent(eventId, reviewer);
        if (comment == null || comment.isBlank()) {
            throw ApiException.badRequest("驳回时必须填写原因");
        }
        transactions.executeWithoutResult(status -> {
            jdbc.update("""
                    UPDATE trace_event SET chain_status='REJECTED', chain_error=?
                    WHERE id=? AND chain_status='PENDING_REVIEW'
                    """, truncate(comment, 500), eventId);
            jdbc.update("""
                    UPDATE event_review
                    SET review_status='REJECTED', reviewer_user_id=?, reviewed_at=CURRENT_TIMESTAMP(6),
                        review_comment=?
                    WHERE event_id=? AND review_status='PENDING_REVIEW'
                    """, reviewer.id(), truncate(comment, 500), eventId);
        });
        audit(reviewer, "REJECT_EVENT", "TRACE_EVENT", eventId, Map.of("comment", comment));
        return loadEvent(eventId);
    }

    private TraceModels.EventView submitApprovedEvent(
            Map<String, Object> row,
            CurrentUser reviewer,
            String comment,
            boolean firstApproval
    ) {
        String eventId = String.valueOf(row.get("id"));
        String batchId = String.valueOf(row.get("batch_id"));
        TraceModels.BatchView batch = loadBatch(batchId);
        ApiModels.EventRequest request = eventRequestFromRow(row, batch);
        rebasePendingEvent(row, batch);

        Map<String, Object> chainPayload = new TreeMap<>();
        chainPayload.put("eventId", eventId);
        chainPayload.put("batchId", batchId);
        chainPayload.put("eventType", row.get("event_type"));
        chainPayload.put("stage", row.get("stage"));
        chainPayload.put("status", row.get("result_status"));
        chainPayload.put("issuerOrgId", String.valueOf(row.get("issuer_org_id")));
        chainPayload.put("operatorUserId", String.valueOf(row.get("operator_user_id")));
        chainPayload.put("businessTime", String.valueOf(row.get("business_time")).replace(' ', 'T'));
        chainPayload.put("locationCode", valueOrEmpty(row.get("location_code")));
        chainPayload.put("dataJson", row.get("payload_json"));
        chainPayload.put("prevEventId", valueOrEmpty(row.get("prev_event_id")));
        chainPayload.put("prevHash", valueOrEmpty(row.get("prev_hash")));
        chainPayload.put("contentHash", row.get("content_hash"));
        chainPayload.put("evidenceRoot", valueOrEmpty(row.get("evidence_root")));

        String msp = jdbc.queryForObject(
                "SELECT msp_id FROM organization WHERE id=?", String.class, row.get("issuer_org_id"));
        try {
            TraceModels.ChainSubmission submission = chain.submit(
                    msp, "AppendEvent", canonicalJson(chainPayload));
            transactions.executeWithoutResult(status -> {
                jdbc.update("""
                        UPDATE trace_event
                        SET chain_status='COMMITTED', tx_id=?, block_number=?, chain_error=NULL
                        WHERE id=?
                        """, submission.transactionId(), submission.blockNumber(), eventId);
                String traceCode = "PACKAGE".equals(request.eventType()) && batch.traceCode() == null
                        ? newTraceCode() : batch.traceCode();
                jdbc.update("""
                        UPDATE trace_batch
                        SET current_stage=?, status=?, last_event_id=?, last_hash=?,
                            trace_code=COALESCE(?, trace_code),
                            batch_type=CASE WHEN ?='PACKAGE' THEN 'FINISHED' ELSE batch_type END,
                            tx_id=?
                        WHERE id=?
                        """, row.get("stage"), row.get("result_status"), eventId,
                        row.get("content_hash"), traceCode, request.eventType(),
                        submission.transactionId(), batchId);
                prepareTransfer(batch, reviewer, request, eventId);
                completeTransfer(batchId, reviewer, request, eventId);
                createInspectionIfNeeded(
                        batchId, eventId, reviewer, request,
                        toLocalDateTime(row.get("business_time")));
                jdbc.update("""
                        UPDATE event_review
                        SET review_status='APPROVED', reviewer_user_id=?, reviewed_at=CURRENT_TIMESTAMP(6),
                            review_comment=?
                        WHERE event_id=?
                        """, reviewer.id(), truncate(comment, 500), eventId);
            });
            audit(reviewer, firstApproval ? "APPROVE_EVENT" : "RETRY_APPROVED_EVENT",
                    "TRACE_EVENT", eventId, Map.of("comment", valueOrEmpty(comment)));
            return loadEvent(eventId);
        } catch (Exception ex) {
            jdbc.update("""
                    UPDATE trace_event SET chain_status='CHAIN_FAILED', chain_error=? WHERE id=?
                    """, truncate(rootMessage(ex), 500), eventId);
            jdbc.update("""
                    UPDATE event_review
                    SET review_status='APPROVED', reviewer_user_id=?, reviewed_at=CURRENT_TIMESTAMP(6),
                        review_comment=?
                    WHERE event_id=?
                    """, reviewer.id(), truncate(comment, 500), eventId);
            throw ApiException.chainError("审核已通过，但上链失败：" + rootMessage(ex));
        }
    }

    private void rebasePendingEvent(Map<String, Object> row, TraceModels.BatchView batch) {
        String currentPrevEventId = valueOrEmpty(batch.lastEventId());
        String currentPrevHash = valueOrEmpty(batch.lastHash());
        String eventPrevEventId = valueOrEmpty(row.get("prev_event_id"));
        String eventPrevHash = valueOrEmpty(row.get("prev_hash"));
        if (Objects.equals(currentPrevEventId, eventPrevEventId)
                && Objects.equals(currentPrevHash, eventPrevHash)) {
            return;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> content = objectMapper.convertValue(
                    objectMapper.readTree(String.valueOf(row.get("payload_json"))),
                    Map.class
            );
            content.put("prevEventId", currentPrevEventId);
            content.put("prevHash", currentPrevHash);
            String payloadJson = canonicalJson(content);
            String contentHash = sha256(payloadJson + "|" + valueOrEmpty(row.get("evidence_root")));
            jdbc.update("""
                    UPDATE trace_event
                    SET prev_event_id=?, prev_hash=?, payload_json=?, content_hash=?
                    WHERE id=? AND chain_status IN ('PENDING_REVIEW','CHAIN_FAILED','FAILED')
                    """, currentPrevEventId, currentPrevHash, payloadJson, contentHash,
                    row.get("id"));
            row.put("prev_event_id", currentPrevEventId);
            row.put("prev_hash", currentPrevHash);
            row.put("payload_json", payloadJson);
            row.put("content_hash", contentHash);
        } catch (Exception ex) {
            throw ApiException.badRequest("无法重算待审核事件的前序哈希：" + rootMessage(ex));
        }
    }

    private Map<String, Object> loadReviewableEvent(String eventId, CurrentUser reviewer) {
        requireOrganizationManager(reviewer);
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT e.* FROM trace_event e
                JOIN event_review er ON er.event_id=e.id
                WHERE e.id=?
                """, eventId);
        if (rows.isEmpty()) {
            throw ApiException.notFound("待审核事件不存在");
        }
        Map<String, Object> row = rows.get(0);
        if (((Number) row.get("issuer_org_id")).longValue() != reviewer.orgId()) {
            throw ApiException.forbidden("只能审核本企业员工提交的数据");
        }
        if (((Number) row.get("operator_user_id")).longValue() == reviewer.id()) {
            throw ApiException.forbidden("不能审核本人提交的数据");
        }
        if (!"PENDING_REVIEW".equals(String.valueOf(row.get("chain_status")))) {
            throw ApiException.badRequest("该提交已审核或处理中");
        }
        return row;
    }

    private ApiModels.EventRequest eventRequestFromRow(Map<String, Object> row, TraceModels.BatchView batch) {
        try {
            JsonNode content = objectMapper.readTree(String.valueOf(row.get("payload_json")));
            Map<String, Object> processData = objectMapper.convertValue(
                    content.path("processData"),
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() { });
            return new ApiModels.EventRequest(
                    String.valueOf(row.get("event_type")),
                    toLocalDateTime(row.get("business_time")),
                    stringValue(row.get("location_code")),
                    processData,
                    List.of(),
                    String.valueOf(row.get("request_id")),
                    parseOptionalLong(content.path("targetOrgId").asText()),
                    parseOptionalLong(content.path("carrierOrgId").asText()),
                    content.path("discrepancyCode").asText(null),
                    stringValue(row.get("correction_of"))
            );
        } catch (Exception ex) {
            throw ApiException.badRequest("提交内容无法解析");
        }
    }

    private Long parseOptionalLong(String value) {
        return value == null || value.isBlank() ? null : Long.valueOf(value);
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        throw ApiException.badRequest("业务时间格式无法解析");
    }

    private void validateTransferRequest(
            TraceModels.BatchView batch,
            CurrentUser user,
            ApiModels.EventRequest request
    ) {
        if ("DISPATCH".equals(request.eventType())) {
            if (request.targetOrgId() == null
                    || Objects.equals(request.targetOrgId(), user.orgId())) {
                throw ApiException.badRequest("发货时必须选择有效的收货企业");
            }
            ensureTargetOrganization(request.targetOrgId(), user.orgType());
            if (request.carrierOrgId() != null) {
                ensureOrganization(request.carrierOrgId(), "LOGISTICS");
            }
        }
        if ("RECEIVE".equals(request.eventType()) && !hasPendingReceipt(batch.id(), user.orgId())) {
            throw ApiException.badRequest("没有待本企业接收的交接单");
        }
        if (Set.of("TRANSPORT", "DELIVER").contains(request.eventType())
                && !hasAssignedCarrierTask(batch.id(), user.orgId())) {
            throw ApiException.forbidden("没有分配给本企业的运输任务");
        }
    }

    private void requireOrganizationManager(CurrentUser user) {
        if (!user.hasRole("ORG_MANAGER") || user.isAdmin()) {
            throw ApiException.forbidden("仅企业管理员可以审核员工提交");
        }
    }

    public TraceModels.EventView retryEvent(String eventId) {
        CurrentUser user = security.currentUser();
        requireOrganizationManager(user);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM trace_event WHERE id = ?", eventId);
        if (rows.isEmpty()) {
            throw ApiException.notFound("事件不存在");
        }
        Map<String, Object> row = rows.get(0);
        if (((Number) row.get("issuer_org_id")).longValue() != user.orgId()) {
            throw ApiException.forbidden("只能重试本企业审核通过的提交");
        }
        if ("COMMITTED".equals(String.valueOf(row.get("chain_status")))) {
            return loadEvent(eventId);
        }
        if (!Set.of("CHAIN_FAILED", "FAILED").contains(String.valueOf(row.get("chain_status")))) {
            throw ApiException.badRequest("该提交当前不允许重试上链");
        }
        return submitApprovedEvent(row, user, "重试上链", false);
    }

    public Map<String, Object> verifyBatch(String batchId) {
        requireBatchAccess(batchId, security.currentUser());
        List<TraceModels.EventView> events = loadEvents(batchId).stream()
                .filter(event -> "COMMITTED".equals(event.chainStatus()))
                .toList();
        List<Map<String, Object>> checks = new ArrayList<>();
        int success = 0;
        for (TraceModels.EventView event : events) {
            boolean valid = false;
            String error = null;
            try {
                valid = chain.verify(event.id(), event.contentHash());
                if (valid) {
                    success++;
                }
            } catch (Exception ex) {
                error = rootMessage(ex);
            }
            Map<String, Object> check = new LinkedHashMap<>();
            check.put("eventId", event.id());
            check.put("eventType", event.eventType());
            check.put("expectedHash", event.contentHash());
            check.put("valid", valid);
            check.put("error", error);
            checks.add(check);
        }
        return Map.of(
                "batchId", batchId,
                "eventCount", events.size(),
                "verifiedCount", success,
                "allVerified", !events.isEmpty() && success == events.size(),
                "checks", checks
        );
    }

    public Map<String, Object> dashboard() {
        CurrentUser user = security.currentUser();
        ApiModels.PageResponse<TraceModels.BatchView> batches = listBatches(null, null, 1, 8);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user", user);
        result.put("batchTotal", batches.total());
        result.put("recentBatches", batches.items());
        long pending = countAccessible(user, "chain_status <> 'COMMITTED'");
        long frozen = countAccessible(user, "status IN ('FROZEN','RECALLED')");
        long inTransit = countAccessible(user, "status = 'IN_TRANSIT'");
        long dispatched = countAccessible(user, "status = 'DISPATCHED'");
        Long pendingReview = jdbc.queryForObject("""
                SELECT COUNT(*) FROM trace_event e
                JOIN event_review er ON er.event_id=e.id
                WHERE e.issuer_org_id=? AND er.review_status='PENDING_REVIEW'
                """, Long.class, user.orgId());
        result.put("pendingChain", pending);
        result.put("riskBatches", frozen);
        result.put("inTransit", inTransit);
        result.put("dispatched", dispatched);
        result.put("pendingReview", pendingReview == null ? 0 : pendingReview);
        result.put("stageCounts", stageCounts(user));
        result.put("recentEvents", recentEvents(user));
        return result;
    }

    public List<Map<String, Object>> activeOrganizations() {
        return jdbc.queryForList("""
                SELECT id, org_code, org_name, org_type
                FROM organization
                WHERE status='ACTIVE' AND org_type IN ('PROCESSOR', 'LOGISTICS')
                ORDER BY FIELD(org_type, 'PROCESSOR', 'LOGISTICS'), id
                """);
    }

    public TraceModels.BatchView loadBatch(String batchId) {
        List<TraceModels.BatchView> rows = jdbc.query("""
                SELECT b.*, o.org_name AS owner_org_name, c.org_name AS custodian_org_name
                FROM trace_batch b
                JOIN organization o ON o.id = b.owner_org_id
                JOIN organization c ON c.id = b.custodian_org_id
                WHERE b.id = ?
                """, (rs, rowNum) -> mapBatch(rs), batchId);
        if (rows.isEmpty()) {
            throw ApiException.notFound("批次不存在");
        }
        return rows.get(0);
    }

    public void requireBatchAccess(String batchId, CurrentUser user) {
        if (user.isAdmin() || user.isRegulator()) {
            return;
        }
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM batch_party WHERE batch_id = ? AND org_id = ?
                """, Integer.class, batchId, user.orgId());
        if (count == null || count == 0) {
            throw ApiException.forbidden("无权查看该批次");
        }
    }

    private void prepareTransfer(
            TraceModels.BatchView batch,
            CurrentUser user,
            ApiModels.EventRequest request,
            String eventId
    ) {
        if ("DISPATCH".equals(request.eventType())) {
            if (request.targetOrgId() == null) {
                throw ApiException.badRequest("发货时必须选择收货企业");
            }
            if (Objects.equals(request.targetOrgId(), user.orgId())) {
                throw ApiException.badRequest("收货企业不能与发货企业相同");
            }
            ensureTargetOrganization(request.targetOrgId(), user.orgType());
            if (request.carrierOrgId() != null) {
                ensureOrganization(request.carrierOrgId(), "LOGISTICS");
            }
            jdbc.update("""
                    INSERT INTO custody_transfer
                        (id, batch_id, event_id, from_org_id, to_org_id, carrier_org_id,
                         status, outbound_at, discrepancy_code)
                    VALUES (?, ?, ?, ?, ?, ?, 'PENDING', ?, ?)
                    """, newId("T"), batch.id(), eventId, user.orgId(), request.targetOrgId(),
                    request.carrierOrgId(), Timestamp.valueOf(LocalDateTime.now()),
                    request.discrepancyCode());
            jdbc.update("""
                    INSERT IGNORE INTO batch_party (batch_id, org_id, party_type, visible_scope)
                    VALUES (?, ?, 'RECEIVER', 'FULL')
                    """, batch.id(), request.targetOrgId());
            if (request.carrierOrgId() != null) {
                jdbc.update("""
                        INSERT IGNORE INTO batch_party (batch_id, org_id, party_type, visible_scope)
                        VALUES (?, ?, 'CARRIER', 'FULL')
                        """, batch.id(), request.carrierOrgId());
            }
        }
        if ("RECEIVE".equals(request.eventType())) {
            Integer count = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM custody_transfer
                    WHERE batch_id=? AND to_org_id=? AND status IN ('PENDING','IN_TRANSIT','DELIVERED')
                    """, Integer.class, batch.id(), user.orgId());
            if (count == null || count == 0) {
                throw ApiException.badRequest("没有待本企业接收的交接单");
            }
        }
        if (Set.of("TRANSPORT", "DELIVER").contains(request.eventType())) {
            Integer count = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM custody_transfer
                    WHERE batch_id=? AND carrier_org_id=? AND status IN ('PENDING','IN_TRANSIT')
                    """, Integer.class, batch.id(), user.orgId());
            if (count == null || count == 0) {
                throw ApiException.forbidden("没有分配给本企业的运输任务");
            }
        }
    }

    private void completeTransfer(
            String batchId,
            CurrentUser user,
            ApiModels.EventRequest request,
            String eventId
    ) {
        if ("DISPATCH".equals(request.eventType())) {
            return;
        }
        if ("TRANSPORT".equals(request.eventType())) {
            jdbc.update("""
                    UPDATE custody_transfer SET status='IN_TRANSIT', event_id=?
                    WHERE batch_id=? AND carrier_org_id=? AND status='PENDING'
                    """, eventId, batchId, user.orgId());
            jdbc.update("UPDATE trace_batch SET custodian_org_id=? WHERE id=?",
                    user.orgId(), batchId);
        }
        if ("DELIVER".equals(request.eventType())) {
            jdbc.update("""
                    UPDATE custody_transfer SET status='DELIVERED', event_id=?
                    WHERE batch_id=? AND carrier_org_id=? AND status='IN_TRANSIT'
                    """, eventId, batchId, user.orgId());
        }
        if ("RECEIVE".equals(request.eventType())) {
            jdbc.update("""
                    UPDATE custody_transfer
                    SET status='RECEIVED', received_at=?, event_id=?, discrepancy_code=?
                    WHERE batch_id=? AND to_org_id=?
                      AND status IN ('PENDING','IN_TRANSIT','DELIVERED')
                    ORDER BY created_at DESC LIMIT 1
                    """, Timestamp.valueOf(LocalDateTime.now()), eventId,
                    request.discrepancyCode(), batchId, user.orgId());
            jdbc.update("UPDATE trace_batch SET custodian_org_id=? WHERE id=?",
                    user.orgId(), batchId);
        }
    }

    private void createInspectionIfNeeded(
            String batchId,
            String eventId,
            CurrentUser user,
            ApiModels.EventRequest request,
            LocalDateTime businessTime
    ) {
        if (!"INSPECTION".equals(request.eventType())) {
            return;
        }
        Map<String, Object> data = request.processData() == null ? Map.of() : request.processData();
        String result = Objects.toString(data.get("overallResult"), "PASS");
        String decision = Objects.toString(data.get("decision"), "RELEASE");
        String sampleNo = Objects.toString(data.get("sampleNo"), "S-" + eventId.substring(2, 10));
        String inspectionType = Objects.toString(data.get("inspectionType"), "SUPERVISION");
        String indicators = canonicalJson(data.getOrDefault("indicators", List.of()));
        jdbc.update("""
                INSERT INTO inspection_record (
                    id, batch_id, event_id, inspection_type, sample_no, indicators_json,
                    overall_result, report_no, report_hash, issued_by, issued_at, decision, remark
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, newId("I"), batchId, eventId, inspectionType, sampleNo, indicators,
                result, Objects.toString(data.get("reportNo"), null),
                Objects.toString(data.get("reportHash"), null), user.orgId(),
                Timestamp.valueOf(businessTime), decision,
                Objects.toString(data.get("remark"), null));
    }

    private void linkEvidence(String batchId, String eventId, List<String> evidenceIds) {
        if (evidenceIds.isEmpty()) {
            return;
        }
        for (String evidenceId : evidenceIds) {
            int updated = jdbc.update("""
                    UPDATE evidence SET event_id=?
                    WHERE id=? AND batch_id=? AND event_id IS NULL
                    """, eventId, evidenceId, batchId);
            if (updated == 0) {
                Integer exists = jdbc.queryForObject("""
                        SELECT COUNT(*) FROM evidence WHERE id=? AND batch_id=?
                        """, Integer.class, evidenceId, batchId);
                if (exists == null || exists == 0) {
                    throw ApiException.badRequest("附件不存在或不属于当前批次：" + evidenceId);
                }
            }
        }
    }

    private String computeEvidenceRoot(String batchId, List<String> evidenceIds) {
        if (evidenceIds.isEmpty()) {
            return null;
        }
        List<String> hashes = new ArrayList<>();
        for (String evidenceId : evidenceIds) {
            List<String> rows = jdbc.queryForList("""
                    SELECT sha256 FROM evidence WHERE id=? AND batch_id=?
                    """, String.class, evidenceId, batchId);
            if (rows.isEmpty()) {
                throw ApiException.badRequest("附件不存在或不属于当前批次：" + evidenceId);
            }
            hashes.add(rows.get(0));
        }
        hashes.sort(Comparator.naturalOrder());
        return sha256(String.join("|", hashes));
    }

    private List<TraceModels.EventView> loadEvents(String batchId) {
        return jdbc.query("""
                SELECT e.*, o.org_name AS issuer_org_name, u.display_name AS operator_name,
                       er.review_status, rv.display_name AS reviewer_name,
                       er.reviewed_at, er.review_comment
                FROM trace_event e
                JOIN organization o ON o.id = e.issuer_org_id
                JOIN sys_user u ON u.id = e.operator_user_id
                LEFT JOIN event_review er ON er.event_id=e.id
                LEFT JOIN sys_user rv ON rv.id=er.reviewer_user_id
                WHERE e.batch_id = ?
                ORDER BY e.business_time ASC, e.created_at ASC
                """, (rs, rowNum) -> mapEvent(rs), batchId);
    }

    private TraceModels.EventView loadEvent(String eventId) {
        List<TraceModels.EventView> rows = jdbc.query("""
                SELECT e.*, o.org_name AS issuer_org_name, u.display_name AS operator_name,
                       er.review_status, rv.display_name AS reviewer_name,
                       er.reviewed_at, er.review_comment
                FROM trace_event e
                JOIN organization o ON o.id = e.issuer_org_id
                JOIN sys_user u ON u.id = e.operator_user_id
                LEFT JOIN event_review er ON er.event_id=e.id
                LEFT JOIN sys_user rv ON rv.id=er.reviewer_user_id
                WHERE e.id = ?
                """, (rs, rowNum) -> mapEvent(rs), eventId);
        if (rows.isEmpty()) {
            throw ApiException.notFound("事件不存在");
        }
        return rows.get(0);
    }

    private List<TraceModels.RelationView> loadRelations(String batchId) {
        return jdbc.query("""
                SELECT r.parent_batch_id, pb.batch_no AS parent_batch_no,
                       r.child_batch_id, cb.batch_no AS child_batch_no,
                       r.relation_type, r.quantity
                FROM batch_relation r
                JOIN trace_batch pb ON pb.id = r.parent_batch_id
                JOIN trace_batch cb ON cb.id = r.child_batch_id
                WHERE r.child_batch_id = ? OR r.parent_batch_id = ?
                ORDER BY r.created_at
                """, (rs, rowNum) -> new TraceModels.RelationView(
                rs.getString("parent_batch_id"), rs.getString("parent_batch_no"),
                rs.getString("child_batch_id"), rs.getString("child_batch_no"),
                rs.getString("relation_type"), rs.getBigDecimal("quantity")
        ), batchId, batchId);
    }

    private List<TraceModels.TransferView> loadTransfers(String batchId) {
        return jdbc.query("""
                SELECT t.*, f.org_name AS from_org_name, d.org_name AS to_org_name,
                       c.org_name AS carrier_org_name
                FROM custody_transfer t
                JOIN organization f ON f.id = t.from_org_id
                JOIN organization d ON d.id = t.to_org_id
                LEFT JOIN organization c ON c.id = t.carrier_org_id
                WHERE t.batch_id = ?
                ORDER BY t.created_at DESC
                """, (rs, rowNum) -> new TraceModels.TransferView(
                rs.getString("id"), rs.getLong("from_org_id"), rs.getString("from_org_name"),
                rs.getLong("to_org_id"), rs.getString("to_org_name"),
                nullableLong(rs, "carrier_org_id"), rs.getString("carrier_org_name"),
                rs.getString("status"), localDateTime(rs, "outbound_at"),
                localDateTime(rs, "received_at"), rs.getString("discrepancy_code")
        ), batchId);
    }

    private List<TraceModels.InspectionView> loadInspections(String batchId) {
        return jdbc.query("""
                SELECT * FROM inspection_record WHERE batch_id=? ORDER BY issued_at DESC
                """, (rs, rowNum) -> new TraceModels.InspectionView(
                rs.getString("id"), rs.getString("inspection_type"),
                rs.getString("sample_no"), readTree(rs.getString("indicators_json")),
                rs.getString("overall_result"), rs.getString("report_no"),
                rs.getString("report_hash"), localDateTime(rs, "issued_at"),
                rs.getString("decision"), rs.getString("remark")
        ), batchId);
    }

    private List<TraceModels.EvidenceView> loadEvidence(String eventId) {
        return jdbc.query("""
                SELECT id, original_name, content_type, file_size, sha256, created_at
                FROM evidence WHERE event_id=? ORDER BY created_at
                """, (rs, rowNum) -> new TraceModels.EvidenceView(
                rs.getString("id"), rs.getString("original_name"),
                rs.getString("content_type"), rs.getLong("file_size"),
                rs.getString("sha256"), localDateTime(rs, "created_at")
        ), eventId);
    }

    private TraceModels.BatchView mapBatch(ResultSet rs) throws SQLException {
        return new TraceModels.BatchView(
                rs.getString("id"),
                rs.getString("batch_no"),
                rs.getString("trace_code"),
                rs.getString("batch_type"),
                rs.getLong("owner_org_id"),
                rs.getString("owner_org_name"),
                rs.getLong("custodian_org_id"),
                rs.getString("custodian_org_name"),
                rs.getString("product_name"),
                rs.getString("spec"),
                rs.getBigDecimal("quantity"),
                rs.getString("unit"),
                rs.getString("current_stage"),
                rs.getString("status"),
                rs.getDate("production_date") == null ? null : rs.getDate("production_date").toLocalDate(),
                rs.getDate("expiry_date") == null ? null : rs.getDate("expiry_date").toLocalDate(),
                rs.getString("last_event_id"),
                rs.getString("last_hash"),
                rs.getString("chain_status"),
                rs.getString("tx_id"),
                localDateTime(rs, "created_at"),
                localDateTime(rs, "updated_at")
        );
    }

    private TraceModels.EventView mapEvent(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        return new TraceModels.EventView(
                id,
                rs.getString("batch_id"),
                rs.getString("event_type"),
                rs.getString("stage"),
                rs.getString("result_status"),
                rs.getLong("issuer_org_id"),
                rs.getString("issuer_org_name"),
                rs.getString("operator_name"),
                localDateTime(rs, "business_time"),
                rs.getString("location_code"),
                readTree(rs.getString("payload_json")).path("processData"),
                rs.getString("content_hash"),
                rs.getString("evidence_root"),
                rs.getString("prev_event_id"),
                rs.getString("prev_hash"),
                rs.getString("chain_status"),
                rs.getString("tx_id"),
                rs.getString("chain_error"),
                localDateTime(rs, "created_at"),
                loadEvidence(id),
                rs.getString("review_status"),
                rs.getString("reviewer_name"),
                localDateTime(rs, "reviewed_at"),
                rs.getString("review_comment")
        );
    }

    private long countAccessible(CurrentUser user, String condition) {
        String access = user.isAdmin() || user.isRegulator()
                ? ""
                : " AND EXISTS (SELECT 1 FROM batch_party p WHERE p.batch_id=b.id AND p.org_id=?)";
        String sql = "SELECT COUNT(*) FROM trace_batch b WHERE " + condition + access;
        Long count = user.isAdmin() || user.isRegulator()
                ? jdbc.queryForObject(sql, Long.class)
                : jdbc.queryForObject(sql, Long.class, user.orgId());
        return count == null ? 0 : count;
    }

    private List<Map<String, Object>> stageCounts(CurrentUser user) {
        String access = user.isAdmin() || user.isRegulator()
                ? ""
                : " AND EXISTS (SELECT 1 FROM batch_party p WHERE p.batch_id=b.id AND p.org_id=?)";
        String sql = """
                SELECT b.current_stage AS stage, COUNT(*) AS total
                FROM trace_batch b WHERE 1=1
                """ + access + " GROUP BY b.current_stage ORDER BY total DESC";
        return user.isAdmin() || user.isRegulator()
                ? jdbc.queryForList(sql)
                : jdbc.queryForList(sql, user.orgId());
    }

    private List<Map<String, Object>> recentEvents(CurrentUser user) {
        String access = user.isAdmin() || user.isRegulator()
                ? ""
                : " AND EXISTS (SELECT 1 FROM batch_party p WHERE p.batch_id=e.batch_id AND p.org_id=?)";
        String sql = """
                SELECT e.id, e.batch_id, b.batch_no, e.event_type, e.stage,
                       e.result_status, e.chain_status, e.business_time
                FROM trace_event e
                JOIN trace_batch b ON b.id=e.batch_id
                WHERE 1=1
                """ + access + " ORDER BY e.created_at DESC LIMIT 10";
        return user.isAdmin() || user.isRegulator()
                ? jdbc.queryForList(sql)
                : jdbc.queryForList(sql, user.orgId());
    }

    private void ensureOrganization(Long orgId, String expectedType) {
        ensureOrganization(orgId, expectedType == null ? Set.of() : Set.of(expectedType), true);
    }

    private void ensureTargetOrganization(Long orgId, String sourceOrgType) {
        Set<String> expectedTypes = switch (sourceOrgType) {
            case "SUPPLIER" -> Set.of("PROCESSOR");
            case "PROCESSOR" -> Set.of("LOGISTICS", "RETAILER");
            default -> Set.of();
        };
        if (expectedTypes.isEmpty()) {
            throw ApiException.forbidden("当前企业不能创建发货交接");
        }
        ensureOrganization(orgId, expectedTypes, false);
    }

    private void ensureOrganization(Long orgId, Set<String> expectedTypes, boolean carrier) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT org_type, status FROM organization WHERE id=?", orgId);
        if (rows.isEmpty() || !"ACTIVE".equals(String.valueOf(rows.get(0).get("status")))) {
            throw ApiException.badRequest("目标企业不存在或已停用");
        }
        if (!expectedTypes.isEmpty()
                && !expectedTypes.contains(String.valueOf(rows.get(0).get("org_type")))) {
            throw ApiException.badRequest(carrier ? "承运企业类型不正确" : "收货企业类型不正确");
        }
    }

    private boolean hasPendingReceipt(String batchId, Long orgId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM custody_transfer
                WHERE batch_id=? AND to_org_id=?
                  AND status IN ('PENDING','IN_TRANSIT','DELIVERED')
                """, Integer.class, batchId, orgId);
        return count != null && count > 0;
    }

    private boolean hasAssignedCarrierTask(String batchId, Long orgId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM custody_transfer
                WHERE batch_id=? AND carrier_org_id=?
                  AND status IN ('PENDING','IN_TRANSIT')
                """, Integer.class, batchId, orgId);
        return count != null && count > 0;
    }

    private void audit(CurrentUser user, String action, String objectType, String objectId, Object detail) {
        try {
            jdbc.update("""
                    INSERT INTO audit_log
                        (user_id, org_id, action, object_type, object_id, detail_json)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, user.id(), user.orgId(), action, objectType, objectId, canonicalJson(detail));
        } catch (Exception ignored) {
            // Audit failure must not hide a successful business transaction.
        }
    }

    private String canonicalJson(Object value) {
        try {
            return canonicalMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("无法序列化业务数据", ex);
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private JsonNode readTree(String value) {
        try {
            return value == null ? objectMapper.createObjectNode() : objectMapper.readTree(value);
        } catch (JsonProcessingException ex) {
            return objectMapper.createObjectNode();
        }
    }

    private String newId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase(Locale.ROOT);
    }

    private String batchNo(String type) {
        String prefix = switch (type) {
            case "RAW_DUCK" -> "YL";
            case "SEMI_FINISHED" -> "BC";
            default -> "CP";
        };
        return prefix + "-" + LocalDateTime.now().format(NUMBER_FORMAT) + "-"
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT);
    }

    private String newTraceCode() {
        return "CDJBY-" + LocalDateTime.now().format(NUMBER_FORMAT) + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }

    private String valueOrEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private String rootMessage(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
