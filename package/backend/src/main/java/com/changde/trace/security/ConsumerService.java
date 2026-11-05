package com.changde.trace.security;

import com.changde.trace.api.ApiException;
import com.changde.trace.api.ApiModels;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ConsumerService {
    private static final Set<String> GENDERS = Set.of("男", "女", "其他", "不公开");

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final SecuritySupport security;

    public ConsumerService(
            JdbcTemplate jdbc,
            PasswordEncoder passwordEncoder,
            SecuritySupport security
    ) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.security = security;
    }

    @Transactional
    public Map<String, Object> register(ApiModels.ConsumerRegisterRequest request) {
        if (!jdbc.queryForList("SELECT id FROM consumer_user WHERE phone=?", request.phone()).isEmpty()) {
            throw ApiException.badRequest("手机号码已注册");
        }
        jdbc.update("""
                INSERT INTO consumer_user (phone, password_hash, nickname, profile_completed)
                VALUES (?, ?, ?, 0)
                """, request.phone(), passwordEncoder.encode(request.password()), "用户" + request.phone().substring(7));
        Long id = jdbc.queryForObject(
                "SELECT id FROM consumer_user WHERE phone=?", Long.class, request.phone());
        return Map.of("consumerId", id, "message", "普通用户注册成功，请登录完善个人信息");
    }

    public ConsumerPrincipal authenticate(String phone, String password) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT id, phone, password_hash, nickname, profile_completed, enabled
                FROM consumer_user WHERE phone=?
                """, phone);
        if (rows.isEmpty() || !passwordEncoder.matches(
                password, String.valueOf(rows.get(0).get("password_hash")))) {
            throw ApiException.forbidden("手机号码或密码错误");
        }
        Map<String, Object> row = rows.get(0);
        if (!asBoolean(row.get("enabled"))) {
            throw ApiException.forbidden("普通用户账号已停用");
        }
        jdbc.update("UPDATE consumer_user SET last_login_at=CURRENT_TIMESTAMP(6) WHERE id=?",
                row.get("id"));
        return new ConsumerPrincipal(
                ((Number) row.get("id")).longValue(),
                String.valueOf(row.get("phone")),
                String.valueOf(row.get("nickname")),
                asBoolean(row.get("profile_completed"))
        );
    }

    public ConsumerPrincipal loadPrincipal(Long consumerId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT id, phone, nickname, profile_completed, enabled
                FROM consumer_user WHERE id=?
                """, consumerId);
        if (rows.isEmpty() || !asBoolean(rows.get(0).get("enabled"))) {
            throw ApiException.forbidden("普通用户账号不可用");
        }
        Map<String, Object> row = rows.get(0);
        return new ConsumerPrincipal(
                ((Number) row.get("id")).longValue(),
                String.valueOf(row.get("phone")),
                String.valueOf(row.get("nickname")),
                asBoolean(row.get("profile_completed"))
        );
    }

    public Map<String, Object> profile() {
        ConsumerPrincipal current = security.currentConsumer();
        return jdbc.queryForMap("""
                SELECT id, phone, nickname, gender, region_code, profile_completed,
                       created_at, last_login_at
                FROM consumer_user WHERE id=?
                """, current.id());
    }

    public Map<String, Object> updateProfile(ApiModels.ConsumerProfileRequest request) {
        ConsumerPrincipal current = security.currentConsumer();
        String gender = request.gender() == null || request.gender().isBlank()
                ? "不公开" : request.gender().trim();
        if (!GENDERS.contains(gender)) {
            throw ApiException.badRequest("性别选项不正确");
        }
        jdbc.update("""
                UPDATE consumer_user
                SET nickname=?, gender=?, region_code=?, profile_completed=1
                WHERE id=?
                """, request.nickname().trim(), gender, request.regionCode(), current.id());
        return Map.of("message", "个人信息已完善", "profileCompleted", true);
    }

    public void changePassword(ApiModels.ConsumerPasswordRequest request) {
        ConsumerPrincipal current = security.currentConsumer();
        String hash = jdbc.queryForObject(
                "SELECT password_hash FROM consumer_user WHERE id=?", String.class, current.id());
        if (!passwordEncoder.matches(request.currentPassword(), hash)) {
            throw ApiException.badRequest("当前密码不正确");
        }
        jdbc.update("UPDATE consumer_user SET password_hash=? WHERE id=?",
                passwordEncoder.encode(request.newPassword()), current.id());
    }

    public List<Map<String, Object>> history() {
        ConsumerPrincipal current = security.currentConsumer();
        return jdbc.queryForList("""
                SELECT b.id AS batchId, b.batch_no AS batchNo, b.trace_code AS traceCode,
                       b.product_name AS productName, b.spec, b.current_stage AS currentStage,
                       b.status, o.org_name AS producerName, h.viewed_at AS viewedAt
                FROM consumer_trace_history h
                JOIN trace_batch b ON b.id=h.batch_id
                JOIN organization o ON o.id=b.owner_org_id
                WHERE h.consumer_id=?
                ORDER BY h.viewed_at DESC
                LIMIT 100
                """, current.id());
    }

    public void recordHistory(String traceCode) {
        ConsumerPrincipal current = security.currentConsumer();
        String batchId = findBatchId(traceCode);
        jdbc.update("""
                INSERT INTO consumer_trace_history (consumer_id, batch_id, trace_code)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE viewed_at=CURRENT_TIMESTAMP(6), trace_code=VALUES(trace_code)
                """, current.id(), batchId, traceCode);
    }

    public List<Map<String, Object>> favorites() {
        ConsumerPrincipal current = security.currentConsumer();
        return jdbc.queryForList("""
                SELECT b.id AS batchId, b.batch_no AS batchNo, b.trace_code AS traceCode,
                       b.product_name AS productName, b.spec, b.current_stage AS currentStage,
                       b.status, o.org_name AS producerName, f.created_at AS favoritedAt
                FROM consumer_favorite f
                JOIN trace_batch b ON b.id=f.batch_id
                JOIN organization o ON o.id=b.owner_org_id
                WHERE f.consumer_id=?
                ORDER BY f.created_at DESC
                """, current.id());
    }

    public Map<String, Object> toggleFavorite(String traceCode) {
        ConsumerPrincipal current = security.currentConsumer();
        String batchId = findBatchId(traceCode);
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM consumer_favorite WHERE consumer_id=? AND batch_id=?
                """, Integer.class, current.id(), batchId);
        boolean favorited;
        if (count != null && count > 0) {
            jdbc.update("DELETE FROM consumer_favorite WHERE consumer_id=? AND batch_id=?",
                    current.id(), batchId);
            favorited = false;
        } else {
            jdbc.update("INSERT INTO consumer_favorite (consumer_id, batch_id) VALUES (?, ?)",
                    current.id(), batchId);
            favorited = true;
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("favorited", favorited);
        response.put("traceCode", traceCode);
        return response;
    }

    private String findBatchId(String traceCode) {
        List<String> rows = jdbc.queryForList(
                "SELECT id FROM trace_batch WHERE trace_code=?", String.class, traceCode);
        if (rows.isEmpty()) {
            throw ApiException.notFound("未找到该追溯码");
        }
        return rows.get(0);
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
