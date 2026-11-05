package com.changde.trace.security;

import com.changde.trace.api.ApiException;
import com.changde.trace.api.ApiModels;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class AccountService {
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
        requireOrgManager(current);
        if (!current.orgId().equals(orgId)) {
            throw ApiException.forbidden("只能查看本企业账号");
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
        if ("SUPPLIER".equals(orgType)) {
            return "SUPPLIER_OPERATOR";
        }
        throw ApiException.badRequest("当前独立节点只支持原料供应商企业");
    }

    private List<String> allowedRoles(String orgType) {
        if ("SUPPLIER".equals(orgType)) {
            return List.of(
                    "ORG_MANAGER",
                    "SUPPLIER_OPERATOR",
                    "SUPPLIER_BREEDER",
                    "SUPPLIER_QA",
                    "SUPPLIER_SHIPPER"
            );
        }
        return List.of();
    }
}
