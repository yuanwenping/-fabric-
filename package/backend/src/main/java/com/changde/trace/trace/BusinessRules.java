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

    public static final Set<String> REGULATORY_EVENTS =
            Set.of("INSPECTION", "FREEZE", "RELEASE", "RECALL");

    private static final Map<String, Set<String>> EVENT_CONTENT_FIELDS = Map.ofEntries(
            Map.entry("BREEDING_RECORD", Set.of(
                    "breedingBatchNo", "feedBatchNo", "vaccineRecord", "medicationRecord",
                    "withdrawalPeriodEnd", "stockQuantity", "healthStatus", "remark")),
            Map.entry("QUARANTINE", Set.of(
                    "quarantineCertNo", "quarantineResult", "quarantineDate", "issuerOrg")),
            Map.entry("DISPATCH", Set.of(
                    "vehicleRef", "destination", "temperature", "sealNo")),
            Map.entry("RECEIVE", Set.of(
                    "quantityReceived", "temperature", "packageIntact", "discrepancyCode")),
            Map.entry("SLAUGHTER", Set.of(
                    "slaughterhouseId", "liveWeight", "carcassWeight",
                    "preSlaughterQuarantineResult", "marinadeStartAt", "marinadeEndAt",
                    "marinadeDurationMin", "brineTemperature", "saltBatchNo")),
            Map.entry("MARINATE", Set.of(
                    "startAt", "endAt", "durationMin", "temperature", "saltBatchNo",
                    "equipmentId", "sensorFileHash")),
            Map.entry("SAUCE", Set.of(
                    "formulaVersion", "startAt", "endAt", "durationMin", "setTemperature",
                    "actualTemperature", "pressureKPa", "pH", "salinity", "ccpResult",
                    "equipmentId")),
            Map.entry("ROAST", Set.of(
                    "processType", "equipmentId", "startAt", "endAt", "setTemperature",
                    "coreTemperature", "humidity", "ccpCompliant", "exceptionCode")),
            Map.entry("QUALITY_CHECK", Set.of(
                    "sampleNo", "inspectionType", "moisture", "microbialResult",
                    "overallResult", "remark")),
            Map.entry("PACKAGE", Set.of(
                    "productCode", "spec", "netWeight", "packageQuantity",
                    "packageMaterialBatchNo", "vacuumDegree", "sealTemperature",
                    "productionLineNo", "qaResult", "expiryDate")),
            Map.entry("WAREHOUSE_IN", Set.of(
                    "warehouseId", "binNo", "quantity", "temperature", "coldChainStatus")),
            Map.entry("WAREHOUSE_OUT", Set.of(
                    "warehouseId", "quantity", "temperature", "outboundOrderNo")),
            Map.entry("TRANSPORT", Set.of(
                    "vehicleRef", "sealNo", "fromPlace", "toPlace", "temperature",
                    "alarmCount", "coldChainCompliant")),
            Map.entry("DELIVER", Set.of(
                    "deliveredAt", "receiptResult", "temperature", "discrepancyCode")),
            Map.entry("SALE", Set.of(
                    "quantitySold", "remainingQuantity", "salesChannel",
                    "saleOrderHash", "storageTemperature")),
            Map.entry("INSPECTION", Set.of(
                    "inspectionType", "sampleNo", "overallResult", "reportNo", "decision",
                    "indicatorName", "indicatorValue", "limitValue", "indicatorResult",
                    "remark", "indicators")),
            Map.entry("FREEZE", Set.of("reasonCode", "decisionScope", "remark")),
            Map.entry("RELEASE", Set.of("reasonCode", "remark")),
            Map.entry("RECALL", Set.of("reasonCode", "recallScope", "remark"))
    );

    private static final Map<String, Set<String>> REQUIRED_CONTENT_FIELDS = Map.ofEntries(
            Map.entry("BREEDING_RECORD", Set.of(
                    "breedingBatchNo", "feedBatchNo", "withdrawalPeriodEnd",
                    "stockQuantity", "healthStatus")),
            Map.entry("QUARANTINE", Set.of(
                    "quarantineCertNo", "quarantineResult", "quarantineDate", "issuerOrg")),
            Map.entry("DISPATCH", Set.of("vehicleRef", "destination")),
            Map.entry("RECEIVE", Set.of("quantityReceived", "packageIntact")),
            Map.entry("SLAUGHTER", Set.of(
                    "slaughterhouseId", "liveWeight", "carcassWeight",
                    "preSlaughterQuarantineResult")),
            Map.entry("MARINATE", Set.of("startAt", "endAt", "durationMin", "temperature")),
            Map.entry("SAUCE", Set.of(
                    "formulaVersion", "startAt", "endAt", "setTemperature",
                    "actualTemperature", "ccpResult")),
            Map.entry("ROAST", Set.of(
                    "processType", "equipmentId", "startAt", "endAt",
                    "coreTemperature", "ccpCompliant")),
            Map.entry("QUALITY_CHECK", Set.of("sampleNo", "inspectionType", "overallResult")),
            Map.entry("PACKAGE", Set.of(
                    "productCode", "spec", "netWeight", "packageQuantity",
                    "qaResult", "expiryDate")),
            Map.entry("WAREHOUSE_IN", Set.of(
                    "warehouseId", "binNo", "quantity", "coldChainStatus")),
            Map.entry("WAREHOUSE_OUT", Set.of("warehouseId", "quantity", "outboundOrderNo")),
            Map.entry("TRANSPORT", Set.of(
                    "vehicleRef", "fromPlace", "toPlace", "temperature", "coldChainCompliant")),
            Map.entry("DELIVER", Set.of("deliveredAt", "receiptResult", "temperature")),
            Map.entry("SALE", Set.of("quantitySold", "salesChannel")),
            Map.entry("INSPECTION", Set.of(
                    "inspectionType", "sampleNo", "overallResult", "reportNo", "decision",
                    "indicatorName", "indicatorValue", "indicatorResult")),
            Map.entry("FREEZE", Set.of("reasonCode", "decisionScope")),
            Map.entry("RELEASE", Set.of("reasonCode")),
            Map.entry("RECALL", Set.of("reasonCode", "recallScope"))
    );

    private static final Map<String, List<String>> ALLOWED_PREVIOUS_STAGES = Map.ofEntries(
            Map.entry("SLAUGHTER", List.of("CREATED")),
            Map.entry("MARINATE", List.of("SLAUGHTERED")),
            Map.entry("SAUCE", List.of("MARINATED", "INSPECTED")),
            Map.entry("ROAST", List.of("SAUCED", "INSPECTED")),
            Map.entry("PACKAGE", List.of("ROASTED", "INSPECTED")),
            Map.entry("WAREHOUSE_IN", List.of("PACKAGED", "WAREHOUSED")),
            Map.entry("WAREHOUSE_OUT", List.of("WAREHOUSED")),
            Map.entry("TRANSPORT", List.of("DISPATCHED")),
            Map.entry("DELIVER", List.of("IN_TRANSIT")),
            Map.entry("RECEIVE", List.of("DISPATCHED", "DELIVERED", "IN_TRANSIT")),
            Map.entry("SALE", List.of("ON_SALE")),
            Map.entry("QUARANTINE", List.of("SOURCE", "ACTIVE")),
            Map.entry("DISPATCH", List.of("SOURCE", "QUARANTINED", "PACKAGED", "WAREHOUSED", "ACTIVE"))
    );

    public static Set<String> allowedEventsFor(CurrentUser user) {
        Set<String> allowed = new LinkedHashSet<>();
        if (user.isAdmin()) {
            return allowed;
        }
        switch (user.orgType()) {
            case "SUPPLIER" -> {
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
            }
            case "PROCESSOR" -> {
                if (user.hasRole("PROCESSOR_OPERATOR")) {
                    allowed.addAll(List.of(
                            "RECEIVE", "SLAUGHTER", "MARINATE", "SAUCE", "ROAST",
                            "PACKAGE", "WAREHOUSE_IN", "WAREHOUSE_OUT", "DISPATCH"));
                }
                if (user.hasRole("PROCESSOR_QA")) {
                    allowed.add("QUALITY_CHECK");
                }
                if (user.hasRole("PROCESSOR_WAREHOUSE")) {
                    allowed.addAll(List.of("WAREHOUSE_IN", "WAREHOUSE_OUT"));
                }
            }
            case "LOGISTICS" -> {
                if (user.hasRole("LOGISTICS_OPERATOR") || user.hasRole("LOGISTICS_DISPATCHER")) {
                    allowed.add("TRANSPORT");
                }
                if (user.hasRole("LOGISTICS_OPERATOR") || user.hasRole("LOGISTICS_DRIVER")) {
                    allowed.addAll(List.of("TRANSPORT", "DELIVER"));
                }
            }
            case "RETAILER" -> {
                if (user.hasRole("RETAILER_OPERATOR") || user.hasRole("RETAIL_RECEIVER")) {
                    allowed.addAll(List.of("RECEIVE", "WAREHOUSE_IN"));
                }
                if (user.hasRole("RETAILER_OPERATOR") || user.hasRole("RETAIL_SALESPERSON")) {
                    allowed.addAll(List.of("WAREHOUSE_OUT", "SALE"));
                }
            }
            case "REGULATOR" -> {
                if (user.hasRole("REGULATOR") || user.hasRole("REGULATOR_INSPECTOR")) {
                    allowed.addAll(List.of("INSPECTION", "FREEZE", "RELEASE", "RECALL"));
                }
            }
            default -> {
            }
        }
        return allowed;
    }

    public static void requireEventPermission(CurrentUser user, String eventType) {
        if (user.isAdmin()) {
            throw ApiException.forbidden("平台管理员不能写入业务事件");
        }
        if (!allowedEventsFor(user).contains(eventType)) {
            throw ApiException.forbidden("当前企业角色不能执行 " + eventType + " 操作");
        }
    }

    public static void validateEventContent(String eventType, Map<String, Object> processData) {
        Set<String> fields = EVENT_CONTENT_FIELDS.get(eventType);
        if (fields == null) {
            throw ApiException.badRequest("该业务类型未开放给岗位员工填写");
        }
        Map<String, Object> content = processData == null ? Map.of() : processData;
        Set<String> unexpected = new LinkedHashSet<>(content.keySet());
        unexpected.removeAll(fields);
        if (!unexpected.isEmpty()) {
            throw ApiException.badRequest(
                    "提交内容包含当前业务不允许的字段：" + String.join(", ", unexpected));
        }
        for (Map.Entry<String, Object> entry : content.entrySet()) {
            boolean inspectionIndicators = "INSPECTION".equals(eventType)
                    && "indicators".equals(entry.getKey());
            if (entry.getValue() instanceof Map<?, ?>
                    || (!inspectionIndicators && entry.getValue() instanceof Collection<?>)) {
                throw ApiException.badRequest(
                        "字段 " + entry.getKey() + " 的数据格式不正确");
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

    private static boolean isBlank(Object value) {
        if (value == null) {
            return true;
        }
        return value instanceof String text && text.isBlank();
    }

    public static String stageFor(String eventType, String orgType) {
        return switch (eventType) {
            case "QUARANTINE" -> "QUARANTINED";
            case "DISPATCH" -> "DISPATCHED";
            case "RECEIVE" -> "RETAILER".equals(orgType) ? "ON_SALE" : "RECEIVED";
            case "SLAUGHTER" -> "SLAUGHTERED";
            case "MARINATE" -> "MARINATED";
            case "SAUCE" -> "SAUCED";
            case "ROAST" -> "ROASTED";
            case "PACKAGE" -> "PACKAGED";
            case "WAREHOUSE_IN" -> "WAREHOUSED";
            case "WAREHOUSE_OUT" -> "DISPATCHED";
            case "TRANSPORT" -> "IN_TRANSIT";
            case "DELIVER" -> "DELIVERED";
            case "SALE" -> "SOLD";
            case "INSPECTION" -> "INSPECTED";
            case "FREEZE" -> "FROZEN";
            case "RECALL" -> "RECALLED";
            default -> null;
        };
    }

    public static String statusFor(String eventType, String orgType, Object inspectionResult) {
        return switch (eventType) {
            case "DISPATCH" -> "DISPATCHED";
            case "RECEIVE" -> "RETAILER".equals(orgType) ? "ON_SALE" : "ACTIVE";
            case "TRANSPORT" -> "IN_TRANSIT";
            case "DELIVER" -> "DELIVERED";
            case "SALE" -> "SOLD";
            case "FREEZE" -> "FROZEN";
            case "RECALL" -> "RECALLED";
            case "RELEASE" -> "ACTIVE";
            case "INSPECTION" -> "FAIL".equalsIgnoreCase(String.valueOf(inspectionResult)) ? "FROZEN" : "ACTIVE";
            default -> "ACTIVE";
        };
    }

    public static void validateStage(String eventType, String currentStage) {
        List<String> allowed = ALLOWED_PREVIOUS_STAGES.get(eventType);
        if (allowed != null && !allowed.contains(currentStage)) {
            throw ApiException.badRequest(
                    "当前阶段 " + currentStage + " 不能执行 " + eventType + "，请按业务流程操作");
        }
    }

    public static String fabricMsp(String orgType) {
        return switch (orgType) {
            case "SUPPLIER" -> "SupplierMSP";
            case "PROCESSOR" -> "ProcessorMSP";
            case "LOGISTICS" -> "LogisticsMSP";
            case "RETAILER" -> "RetailerMSP";
            case "REGULATOR" -> "RegulatorMSP";
            case "ADMIN" -> "SupplierMSP";
            default -> throw ApiException.badRequest("不支持的企业类型：" + orgType);
        };
    }
}
