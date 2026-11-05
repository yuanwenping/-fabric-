package com.changde.trace.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DatabaseInitializer implements ApplicationRunner {
    private static final List<String> LEGACY_DEMO_USERS =
            List.of("supplier", "processor", "quality", "logistics", "retailer", "regulator", "admin");

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final String bootstrapAdminUsername;
    private final String bootstrapAdminPassword;

    public DatabaseInitializer(
            JdbcTemplate jdbc,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-admin.username}") String bootstrapAdminUsername,
            @Value("${app.bootstrap-admin.password}") String bootstrapAdminPassword
    ) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapAdminUsername = bootstrapAdminUsername;
        this.bootstrapAdminPassword = bootstrapAdminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        disableLegacyDemoUsers();
        ensurePlatformAdmin();
    }

    private void disableLegacyDemoUsers() {
        for (String username : LEGACY_DEMO_USERS) {
            List<Long> ids = jdbc.queryForList(
                    "SELECT id FROM sys_user WHERE username = ?", Long.class, username);
            for (Long id : ids) {
                String legacyUsername = "legacy_" + username;
                Integer collision = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM sys_user WHERE username = ?", Integer.class, legacyUsername);
                jdbc.update("UPDATE sys_user SET enabled = 0 WHERE id = ?", id);
                if (collision == null || collision == 0) {
                    jdbc.update("UPDATE sys_user SET username = ?, display_name = ? WHERE id = ?",
                            legacyUsername, "历史审计账号-" + username, id);
                }
            }
        }
    }

    private void ensurePlatformAdmin() {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM sys_user u
                JOIN sys_user_role ur ON ur.user_id = u.id
                JOIN sys_role r ON r.id = ur.role_id
                WHERE r.role_code = 'PLATFORM_ADMIN' AND u.enabled = 1
                """, Integer.class);
        if (count != null && count > 0) {
            return;
        }

        Long orgId = jdbc.queryForObject(
                "SELECT id FROM organization WHERE org_code = 'PLATFORM001'", Long.class);
        Long roleId = jdbc.queryForObject(
                "SELECT id FROM sys_role WHERE role_code = 'PLATFORM_ADMIN'", Long.class);
        List<Long> existing = jdbc.queryForList(
                "SELECT id FROM sys_user WHERE username = ?", Long.class, bootstrapAdminUsername);
        Long userId;
        if (existing.isEmpty()) {
            jdbc.update("""
                    INSERT INTO sys_user (username, password_hash, display_name, org_id, enabled)
                    VALUES (?, ?, '平台管理员', ?, 1)
                    """, bootstrapAdminUsername, passwordEncoder.encode(bootstrapAdminPassword), orgId);
            userId = jdbc.queryForObject(
                    "SELECT id FROM sys_user WHERE username = ?", Long.class, bootstrapAdminUsername);
        } else {
            userId = existing.get(0);
            jdbc.update("""
                    UPDATE sys_user SET password_hash=?, display_name='平台管理员', org_id=?, enabled=1
                    WHERE id=?
                    """, passwordEncoder.encode(bootstrapAdminPassword), orgId, userId);
        }
        jdbc.update("INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (?, ?)", userId, roleId);
    }
}
