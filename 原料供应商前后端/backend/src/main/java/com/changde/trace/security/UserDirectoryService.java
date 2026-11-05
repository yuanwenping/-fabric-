package com.changde.trace.security;

import com.changde.trace.api.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class UserDirectoryService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final String nodeOrgCode;

    public UserDirectoryService(
            JdbcTemplate jdbc,
            PasswordEncoder passwordEncoder,
            @Value("${app.node.org-code}") String nodeOrgCode
    ) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.nodeOrgCode = nodeOrgCode;
    }

    public CurrentUser authenticate(String username, String password) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT u.id, u.username, u.password_hash, u.display_name, u.enabled,
                       o.id AS org_id, o.org_code, o.org_name, o.org_type, o.status AS org_status
                FROM sys_user u
                JOIN organization o ON o.id = u.org_id
                WHERE u.username = ?
                """, username);
        if (rows.isEmpty()) {
            throw ApiException.forbidden("用户名或密码错误");
        }
        Map<String, Object> row = rows.get(0);
        if (!passwordEncoder.matches(password, String.valueOf(row.get("password_hash")))) {
            throw ApiException.forbidden("用户名或密码错误");
        }
        if (!asBoolean(row.get("enabled"))) {
            if ("PENDING".equals(String.valueOf(row.get("org_status")))) {
                throw ApiException.forbidden("企业注册正在等待平台管理员审核");
            }
            throw ApiException.forbidden("账号已被停用");
        }
        if (!"ACTIVE".equals(String.valueOf(row.get("org_status")))) {
            throw ApiException.forbidden("所属企业状态异常，当前不能登录");
        }
        requireNodeOrganization(row);
        return toCurrentUser(row);
    }

    public CurrentUser load(Long userId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT u.id, u.username, u.display_name, u.enabled,
                       o.id AS org_id, o.org_code, o.org_name, o.org_type, o.status AS org_status
                FROM sys_user u
                JOIN organization o ON o.id = u.org_id
                WHERE u.id = ?
                """, userId);
        if (rows.isEmpty()
                || !asBoolean(rows.get(0).get("enabled"))
                || !"ACTIVE".equals(String.valueOf(rows.get(0).get("org_status")))) {
            throw ApiException.forbidden("账号不可用");
        }
        requireNodeOrganization(rows.get(0));
        return toCurrentUser(rows.get(0));
    }

    private void requireNodeOrganization(Map<String, Object> row) {
        if (!nodeOrgCode.equals(String.valueOf(row.get("org_code")))) {
            throw ApiException.forbidden("当前节点只允许所属企业 " + nodeOrgCode + " 的账号登录");
        }
    }

    private CurrentUser toCurrentUser(Map<String, Object> row) {
        Long userId = ((Number) row.get("id")).longValue();
        Set<String> roles = new LinkedHashSet<>(jdbc.queryForList("""
                SELECT r.role_code
                FROM sys_user_role ur
                JOIN sys_role r ON r.id = ur.role_id
                WHERE ur.user_id = ?
                """, String.class, userId));
        return new CurrentUser(
                userId,
                String.valueOf(row.get("username")),
                String.valueOf(row.get("display_name")),
                ((Number) row.get("org_id")).longValue(),
                String.valueOf(row.get("org_code")),
                String.valueOf(row.get("org_name")),
                String.valueOf(row.get("org_type")),
                roles
        );
    }

    private boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() == 1;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
