package com.changde.trace.security;

import com.changde.trace.api.ApiException;
import com.changde.trace.api.ApiModels;
import com.changde.trace.trace.BusinessRules;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AccountService {
    private static final Set<String> SELF_SERVICE_ORG_TYPES =
            Set.of("SUPPLIER", "PROCESSOR", "LOGISTICS", "RETAILER");

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final SecuritySupport security;

    public AccountService(
            JdbcTemplate jdbc,
            PasswordEncoder passwordEncoder,
            SecuritySupport security
    ) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.security = security;
    }

    @Transactional
    public Map<String, Object> registerEnterprise(ApiModels.EnterpriseRegisterRequest request) {
        String orgType = request.orgType().trim().toUpperCase();
        if (!SELF_SERVICE_ORG_TYPES.contains(orgType)) {
            throw ApiException.forbidden("监管部门和平台管理账号只能由平台管理员创建");
        }
        if (!jdbc.queryForList("SELECT id FROM sys_user WHERE username=?", request.phone()).isEmpty()) {
            throw ApiException.badRequest("手机号码已注册");
        }
        String orgCode = normalizeOrgCode(orgType, request.orgCode());
        if (!jdbc.queryForList("SELECT id FROM organization WHERE org_code=?", orgCode).isEmpty()) {
            throw ApiException.badRequest("企业编号已存在");
        }

        jdbc.update("""
                INSERT INTO organization
                    (org_code, org_name, org_type, region_code, msp_id, status)
                VALUES (?, ?, ?, ?, ?, 'PENDING')
                """, orgCode, request.orgName().trim(), orgType, request.regionCode(),
                BusinessRules.fabricMsp(orgType));
        Long orgId = jdbc.queryForObject(
                "SELECT id FROM organization WHERE org_code = ?", Long.class, orgCode);
        jdbc.update("""
                INSERT INTO sys_user
                    (username, password_hash, display_name, org_id, enabled)
                VALUES (?, ?, ?, ?, 0)
                """, request.phone(), passwordEncoder.encode(request.password()),
                request.displayName(), orgId);
        Long userId = jdbc.queryForObject(
                "SELECT id FROM sys_user WHERE username=?", Long.class, request.phone());

        addRole(userId, roleForOrgType(orgType));
        addRole(userId, "ORG_MANAGER");
        return Map.of(
                "orgId", orgId,
                "userId", userId,
                "orgCode", orgCode,
                "status", "PENDING",
                "message", "企业注册申请已提交，请等待平台管理员审核"
        );
    }

    public void changePassword(ApiModels.ChangePasswordRequest request) {
        CurrentUser user = security.currentUser();
        String hash = jdbc.queryForObject(
                "SELECT password_hash FROM sys_user WHERE id=?", String.class, user.id());
        if (!passwordEncoder.matches(request.currentPassword(), hash)) {
            throw ApiException.badRequest("当前密码不正确");
        }
        jdbc.update("UPDATE sys_user SET password_hash=? WHERE id=?",
                passwordEncoder.encode(request.newPassword()), user.id());
    }

    public List<Map<String, Object>> enterpriseUsers(Long orgId) {
        CurrentUser current = security.currentUser();
        if (!current.isAdmin()) {
            requireOrgManager(current);
            if (!current.orgId().equals(orgId)) {
                throw ApiException.forbidden("只能查看本企业账号");
            }
        }
        return jdbc.queryForList("""
                SELECT u.id, u.username, u.display_name, u.enabled, u.created_at,
                       GROUP_CONCAT(r.role_code ORDER BY r.role_code) AS roles
                FROM sys_user u
                LEFT JOIN sys_user_role ur ON ur.user_id = u.id
                LEFT JOIN sys_role r ON r.id = ur.role_id
                WHERE u.org_id = ?
                GROUP BY u.id, u.username, u.display_name, u.enabled, u.created_at
                ORDER BY u.id
                """, orgId);
    }

    @Transactional
    public Map<String, Object> createEnterpriseUser(ApiModels.OrgUserCreateRequest request) {
        CurrentUser current = security.currentUser();
        requireOrgManager(current);
        if (current.isAdmin()) {
            throw ApiException.forbidden("该组织不能使用企业账号管理接口");
        }
        String roleCode = request.roleCode().toUpperCase();
        if (!allowedRoles(current.orgType()).contains(roleCode)) {
            throw ApiException.forbidden("当前企业不能分配该角色");
        }
        if (!jdbc.queryForList("SELECT id FROM sys_user WHERE username=?", request.phone()).isEmpty()) {
            throw ApiException.badRequest("手机号码已注册");
        }
        jdbc.update("""
                INSERT INTO sys_user (username, password_hash, display_name, org_id, enabled)
                VALUES (?, ?, ?, ?, 1)
                """, request.phone(), passwordEncoder.encode(request.password()),
                request.displayName(), current.orgId());
        Long userId = jdbc.queryForObject(
                "SELECT id FROM sys_user WHERE username=?", Long.class, request.phone());
        addRoleWithBase(userId, roleCode, current.orgType());
        return Map.of("id", userId, "username", request.phone(), "roleCode", roleCode);
    }

    @Transactional
    public Map<String, Object> createPlatformUser(ApiModels.UserCreateRequest request) {
        CurrentUser current = security.currentUser();
        if (!current.isAdmin()) {
            throw ApiException.forbidden("仅平台管理员可以创建企业和监管账号");
        }
        String username = request.username().trim();
        String roleCode = request.roleCode().trim().toUpperCase();
        if (!"PLATFORM_ADMIN".equals(roleCode)
                && !username.matches("^1[3-9]\\d{9}$")) {
            throw ApiException.badRequest("企业和监管账号必须使用11位手机号码");
        }
        if (!jdbc.queryForList("SELECT id FROM sys_user WHERE username=?", username).isEmpty()) {
            throw ApiException.badRequest("用户名已存在");
        }
        List<Map<String, Object>> organizations = jdbc.queryForList(
                "SELECT id, org_type FROM organization WHERE id=? AND status='ACTIVE'",
                request.orgId());
        if (organizations.isEmpty()) {
            throw ApiException.badRequest("所属企业不存在或已停用");
        }
        String orgType = String.valueOf(organizations.get(0).get("org_type"));
        if (!allowedRoles(orgType).contains(roleCode)) {
            throw ApiException.badRequest("该角色不能分配给当前类型的企业");
        }
        jdbc.update("""
                INSERT INTO sys_user (username, password_hash, display_name, org_id)
                VALUES (?, ?, ?, ?)
                """, username, passwordEncoder.encode(request.password()),
                request.displayName(), request.orgId());
        Long userId = jdbc.queryForObject(
                "SELECT id FROM sys_user WHERE username=?", Long.class, username);
        addRoleWithBase(userId, roleCode, orgType);
        return Map.of(
                "id", userId,
                "username", username,
                "roleCode", roleCode,
                "orgId", request.orgId()
        );
    }

    public void setEnterpriseUserStatus(Long userId, boolean enabled) {
        CurrentUser current = security.currentUser();
        requireOrgManager(current);
        if (current.id().equals(userId)) {
            throw ApiException.badRequest("不能停用当前登录账号");
        }
        ensureOwnedUser(userId, current.orgId());
        jdbc.update("UPDATE sys_user SET enabled=? WHERE id=?", enabled, userId);
    }

    public void resetEnterprisePassword(Long userId, String newPassword) {
        CurrentUser current = security.currentUser();
        requireOrgManager(current);
        ensureOwnedUser(userId, current.orgId());
        jdbc.update("UPDATE sys_user SET password_hash=? WHERE id=?",
                passwordEncoder.encode(newPassword), userId);
    }

    public List<Map<String, Object>> allowedRolesForCurrentOrganization() {
        CurrentUser current = security.currentUser();
        requireOrgManager(current);
        List<String> roles = List.copyOf(allowedRoles(current.orgType()));
        if (roles.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", roles.stream().map(role -> "?").toList());
        return jdbc.query("""
                SELECT role_code, role_name FROM sys_role
                WHERE role_code IN (%s) ORDER BY id
                """.formatted(placeholders),
                (rs, rowNum) -> Map.of(
                        "role_code", rs.getString("role_code"),
                        "role_name", rs.getString("role_name")),
                roles.toArray());
    }

    private void ensureOwnedUser(Long userId, Long orgId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE id=? AND org_id=?",
                Integer.class, userId, orgId);
        if (count == null || count == 0) {
            throw ApiException.forbidden("只能管理本企业账号");
        }
    }

    private void requireOrgManager(CurrentUser user) {
        if (!user.hasRole("ORG_MANAGER")) {
            throw ApiException.forbidden("仅企业主账号可以管理本企业员工");
        }
    }

    private void addRole(Long userId, String roleCode) {
        Long roleId = jdbc.queryForObject(
                "SELECT id FROM sys_role WHERE role_code=?", Long.class, roleCode);
        jdbc.update("INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (?, ?)",
                userId, roleId);
    }

    private void addRoleWithBase(Long userId, String roleCode, String orgType) {
        addRole(userId, roleCode);
        if ("ORG_MANAGER".equals(roleCode)) {
            addRole(userId, roleForOrgType(orgType));
        }
    }

    private String roleForOrgType(String orgType) {
        return switch (orgType) {
            case "SUPPLIER" -> "SUPPLIER_OPERATOR";
            case "PROCESSOR" -> "PROCESSOR_OPERATOR";
            case "LOGISTICS" -> "LOGISTICS_OPERATOR";
            case "RETAILER" -> "RETAILER_OPERATOR";
            case "REGULATOR" -> "REGULATOR";
            case "ADMIN" -> "PLATFORM_ADMIN";
            default -> throw ApiException.badRequest("不支持的企业类型");
        };
    }

    private List<String> allowedRoles(String orgType) {
        return switch (orgType) {
            case "SUPPLIER" -> List.of(
                    "ORG_MANAGER",
                    "SUPPLIER_OPERATOR",
                    "SUPPLIER_BREEDER",
                    "SUPPLIER_QA",
                    "SUPPLIER_SHIPPER"
            );
            case "PROCESSOR" -> List.of(
                    "ORG_MANAGER",
                    "PROCESSOR_OPERATOR",
                    "PROCESSOR_QA",
                    "PROCESSOR_WAREHOUSE"
            );
            case "LOGISTICS" -> List.of(
                    "ORG_MANAGER",
                    "LOGISTICS_OPERATOR",
                    "LOGISTICS_DISPATCHER",
                    "LOGISTICS_DRIVER"
            );
            case "RETAILER" -> List.of(
                    "ORG_MANAGER",
                    "RETAILER_OPERATOR",
                    "RETAIL_RECEIVER",
                    "RETAIL_SALESPERSON"
            );
            case "REGULATOR" -> List.of(
                    "ORG_MANAGER",
                    "REGULATOR",
                    "REGULATOR_INSPECTOR"
            );
            case "ADMIN" -> List.of("PLATFORM_ADMIN");
            default -> List.of();
        };
    }

    private String normalizeOrgCode(String orgType, String requestedCode) {
        String prefix = switch (orgType) {
            case "SUPPLIER" -> "SUP";
            case "PROCESSOR" -> "PRO";
            case "LOGISTICS" -> "LOG";
            case "RETAILER" -> "RET";
            case "REGULATOR" -> "REG";
            case "ADMIN" -> "PLATFORM";
            default -> throw ApiException.badRequest("不支持的企业类型");
        };
        String suffix = requestedCode == null ? "" : requestedCode.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9_-]", "");
        if (suffix.startsWith(prefix + "-")) {
            return suffix;
        }
        if (suffix.isEmpty()) {
            String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            for (int attempt = 0; attempt < 20; attempt++) {
                String random = UUID.randomUUID().toString().replace("-", "")
                        .substring(0, 4).toUpperCase(Locale.ROOT);
                String candidate = prefix + "-" + date + "-" + random;
                if (jdbc.queryForList("SELECT id FROM organization WHERE org_code=?", candidate).isEmpty()) {
                    return candidate;
                }
            }
            throw ApiException.badRequest("企业编号生成失败，请手动填写");
        }
        return prefix + "-" + suffix;
    }
}
