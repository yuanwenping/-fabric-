package com.changde.trace.trace;

import com.changde.trace.api.ApiException;
import com.changde.trace.security.CurrentUser;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BusinessRules {
    private BusinessRules() {
    }

    public static final Set<String> REGULATORY_EVENTS = Set.of();

    private static final Map<String, Set<String>> EVENT_CONTENT_FIELDS = Map.of(
            "BREEDING_RECORD", Set.of(
                    "breedingBatchNo", "feedBatchNo", "vaccineRecord", "medicationRecord",
                    "withdrawalPeriodEnd", "stockQuantity", "healthStatus", "remark"),
            "QUARANTINE", Set.of(
                    "quarantineCertNo", "quarantineResult", "quarantineDate", "issuerOrg"),
            "QUALITY_CHECK", Set.of(
                    "sampleNo", "inspectionType", "moisture", "microbialResult",
                    "overallResult", "remark"),
            "DISPATCH", Set.of(
                    "vehicleRef", "destination", "temperature", "sealNo")
    );

    private static final Map<String, Set<String>> REQUIRED_CONTENT_FIELDS = Map.of(
            "BREEDING_RECORD", Set.of(
                    "breedingBatchNo", "feedBatchNo", "withdrawalPeriodEnd",
                    "stockQuantity", "healthStatus"),
            "QUARANTINE", Set.of(
                    "quarantineCertNo", "quarantineResult", "quarantineDate", "issuerOrg"),
            "QUALITY_CHECK", Set.of("sampleNo", "inspectionType", "overallResult"),
            "DISPATCH", Set.of("vehicleRef", "destination")
    );

    private static final Map<String, List<String>> ALLOWED_PREVIOUS_STAGES = Map.of(
            "QUARANTINE", List.of("SOURCE", "ACTIVE"),
            "DISPATCH", List.of("SOURCE", "QUARANTINED", "ACTIVE")
    );

    public static Set<String> allowedEventsFor(CurrentUser user) {
        Set<String> allowed = new LinkedHashSet<>();
        if (!"SUPPLIER".equals(user.orgType())) {
            return allowed;
        }
        if (user.hasRole("SUPPLIER_OPERATOR")) {
            allowed.addAll(List.of("QUARANTINE", "DISPATCH"));
        }
        if (user.hasRole("SUPPLIER_BREEDER")) {
            allowed.add("BREEDING_RECORD");
        }
        if (user.hasRole("SUPPLIER_QA")) {
            allowed.add("QUALITY_CHECK");
        }
        if (user.hasRole("SUPPLIER_SHIPPER")) {
            allowed.add("DISPATCH");
        }
        return allowed;
    }

    public static void requireEventPermission(CurrentUser user, String eventType) {
        if (!"SUPPLIER".equals(user.orgType())) {
            throw ApiException.forbidden("当前节点只负责原料供应商业务");
        }
        if (!allowedEventsFor(user).contains(eventType)) {
            throw ApiException.forbidden("当前岗位不能执行该业务操作");
        }
    }

    public static void validateEventContent(String eventType, Map<String, Object> processData) {
        Set<String> fields = EVENT_CONTENT_FIELDS.get(eventType);
        if (fields == null) {
            throw ApiException.badRequest("该业务类型未开放给原料供应商岗位");
        }
        Map<String, Object> content = processData == null ? Map.of() : processData;
        Set<String> unexpected = new LinkedHashSet<>(content.keySet());
        unexpected.removeAll(fields);
        if (!unexpected.isEmpty()) {
            throw ApiException.badRequest(
                    "提交内容包含当前业务不允许的字段：" + String.join(", ", unexpected));
        }
        for (Map.Entry<String, Object> entry : content.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?>
                    || entry.getValue() instanceof Collection<?>) {
                throw ApiException.badRequest("字段 " + entry.getKey() + " 的数据格式不正确");
            }
        }
        List<String> missing = REQUIRED_CONTENT_FIELDS.getOrDefault(eventType, Set.of()).stream()
                .filter(field -> isBlank(content.get(field)))
                .toList();
        if (!missing.isEmpty()) {
            throw ApiException.badRequest(
                    "请填写当前业务的必填字段：" + String.join(", ", missing));
        }
    }

    public static String stageFor(String eventType) {
        return switch (eventType) {
            case "QUARANTINE" -> "QUARANTINED";
            case "DISPATCH" -> "DISPATCHED";
            default -> null;
        };
    }

    public static String statusFor(String eventType) {
        return "DISPATCH".equals(eventType) ? "DISPATCHED" : "ACTIVE";
    }

    public static void validateStage(String eventType, String currentStage) {
        List<String> allowed = ALLOWED_PREVIOUS_STAGES.get(eventType);
        if (allowed != null && !allowed.contains(currentStage)) {
            throw ApiException.badRequest(
                    "当前阶段 " + currentStage + " 不能执行该业务，请按流程操作");
        }
    }

    private static boolean isBlank(Object value) {
        if (value == null) {
            return true;
        }
        return value instanceof String text && text.isBlank();
    }
}
