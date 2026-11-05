package com.changde.trace.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class ApiModels {
    private ApiModels() {
    }

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 72, message = "新密码长度应为8到72位") String newPassword
    ) {
    }

    public record OrgUserCreateRequest(
            @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的11位手机号码") String phone,
            @NotBlank @Size(min = 8, max = 72, message = "密码长度应为8到72位") String password,
            @NotBlank String displayName,
            @NotBlank String roleCode
    ) {
    }

    public record PasswordResetRequest(
            @NotBlank @Size(min = 8, max = 72, message = "密码长度应为8到72位") String newPassword
    ) {
    }

    public record ReviewRequest(
            String comment
    ) {
    }

    public record BatchCreateRequest(
            @NotBlank String batchType,
            String productName,
            String spec,
            @NotNull @DecimalMin("0.001") BigDecimal quantity,
            @NotBlank String unit,
            LocalDate productionDate,
            LocalDate expiryDate,
            List<String> parentBatchIds,
            String relationType
    ) {
    }

    public record EventRequest(
            @NotBlank String eventType,
            LocalDateTime businessTime,
            String locationCode,
            Map<String, Object> processData,
            List<String> evidenceIds,
            @NotBlank String requestId,
            Long targetOrgId,
            Long carrierOrgId,
            String discrepancyCode,
            String correctionOf
    ) {
    }

    public record PageResponse<T>(
            List<T> items,
            long total,
            int page,
            int size
    ) {
    }
}
