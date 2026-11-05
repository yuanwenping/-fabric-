package com.changde.trace.api;

import com.changde.trace.security.CurrentUser;
import com.changde.trace.security.AccountService;
import com.changde.trace.security.SecuritySupport;
import jakarta.validation.Valid;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final JdbcTemplate jdbc;
    private final AccountService accounts;
    private final PasswordEncoder passwordEncoder;
    private final SecuritySupport security;

    public AdminController(
            JdbcTemplate jdbc,
            AccountService accounts,
            PasswordEncoder passwordEncoder,
            SecuritySupport security
    ) {
        this.jdbc = jdbc;
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.security = security;
    }

    @GetMapping("/organizations")
    public List<Map<String, Object>> organizations() {
        requireAdmin();
        return jdbc.queryForList("""
                SELECT id, org_code, org_name, org_type, region_code, msp_id, status, created_at,
                       (SELECT COUNT(*) FROM sys_user u WHERE u.org_id=organization.id AND u.enabled=1) AS active_users
                FROM organization ORDER BY id
                """);
    }

    @GetMapping("/users")
    public List<Map<String, Object>> users() {
        requireAdmin();
        return jdbc.queryForList("""
                SELECT u.id, u.username, u.display_name, u.enabled, u.created_at,
                       o.org_code, o.org_name, o.org_type, o.status AS org_status,
                       GROUP_CONCAT(r.role_code ORDER BY r.role_code) AS roles
                FROM sys_user u
                JOIN organization o ON o.id = u.org_id
                LEFT JOIN sys_user_role ur ON ur.user_id = u.id
                LEFT JOIN sys_role r ON r.id = ur.role_id
                WHERE u.username NOT REGEXP '^legacy_'
                GROUP BY u.id, u.username, u.display_name, u.enabled, u.created_at,
                         o.org_code, o.org_name, o.org_type, o.status
                ORDER BY u.id
                """);
    }

    @PostMapping("/users")
    public Map<String, Object> createUser(@Valid @RequestBody ApiModels.UserCreateRequest request) {
        requireAdmin();
        return accounts.createPlatformUser(request);
    }

    @PutMapping("/users/{userId}/status")
    public Map<String, Object> setUserStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, Boolean> request
    ) {
        requireAdmin();
        CurrentUser current = security.currentUser();
        if (current.id().equals(userId)) {
            throw ApiException.badRequest("不能停用当前登录账号");
        }
        boolean enabled = Boolean.TRUE.equals(request.get("enabled"));
        jdbc.update("UPDATE sys_user SET enabled=? WHERE id=?", enabled, userId);
        return Map.of("id", userId, "enabled", enabled);
    }

    @PutMapping("/users/{userId}/password")
    public Map<String, Object> resetPassword(
            @PathVariable Long userId,
            @Valid @RequestBody ApiModels.PasswordResetRequest request
    ) {
        requireAdmin();
        int updated = jdbc.update("UPDATE sys_user SET password_hash=? WHERE id=?",
                passwordEncoder.encode(request.newPassword()), userId);
        if (updated == 0) {
            throw ApiException.notFound("用户不存在");
        }
        return Map.of("id", userId, "message", "密码已重置");
    }

    @PutMapping("/organizations/{orgId}/approve")
    public Map<String, Object> approveOrganization(@PathVariable Long orgId) {
        requireAdmin();
        int updated = jdbc.update("""
                UPDATE organization SET status='ACTIVE'
                WHERE id=? AND status='PENDING'
                """, orgId);
        if (updated == 0) {
            throw ApiException.badRequest("企业不存在或不是待审核状态");
        }
        jdbc.update("UPDATE sys_user SET enabled=1 WHERE org_id=?", orgId);
        return Map.of("orgId", orgId, "status", "ACTIVE");
    }

    @PutMapping("/organizations/{orgId}/reject")
    public Map<String, Object> rejectOrganization(@PathVariable Long orgId) {
        requireAdmin();
        int updated = jdbc.update("""
                UPDATE organization SET status='REJECTED'
                WHERE id=? AND status='PENDING'
                """, orgId);
        if (updated == 0) {
            throw ApiException.badRequest("企业不存在或不是待审核状态");
        }
        jdbc.update("UPDATE sys_user SET enabled=0 WHERE org_id=?", orgId);
        return Map.of("orgId", orgId, "status", "REJECTED");
    }

    @GetMapping("/roles")
    public List<Map<String, Object>> roles() {
        requireAdmin();
        return jdbc.queryForList("SELECT id, role_code, role_name FROM sys_role ORDER BY id");
    }

    private void requireAdmin() {
        if (!security.currentUser().isAdmin()) {
            throw ApiException.forbidden("仅平台管理员可以执行该操作");
        }
    }
}
