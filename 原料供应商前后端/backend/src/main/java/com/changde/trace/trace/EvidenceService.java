package com.changde.trace.trace;

import com.changde.trace.api.ApiException;
import com.changde.trace.security.CurrentUser;
import com.changde.trace.security.SecuritySupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class EvidenceService {
    private final JdbcTemplate jdbc;
    private final SecuritySupport security;
    private final TraceService traces;
    private final Path uploadRoot;

    public EvidenceService(
            JdbcTemplate jdbc,
            SecuritySupport security,
            TraceService traces,
            @Value("${app.upload-dir:./uploads}") String uploadDir
    ) throws IOException {
        this.jdbc = jdbc;
        this.security = security;
        this.traces = traces;
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadRoot);
    }

    public TraceModels.EvidenceView upload(String batchId, MultipartFile file) {
        CurrentUser user = security.currentUser();
        traces.requireBatchAccess(batchId, user);
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("请选择要上传的文件");
        }
        String original = file.getOriginalFilename() == null ? "evidence.bin" : file.getOriginalFilename();
        String extension = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0 && original.length() - dot <= 12) {
            extension = original.substring(dot).replaceAll("[^A-Za-z0-9.]", "");
        }
        String evidenceId = "V-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20)
                .toUpperCase(Locale.ROOT);
        String relative = batchId + "/" + evidenceId + extension;
        Path target = uploadRoot.resolve(relative).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw ApiException.badRequest("非法文件路径");
        }
        try {
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            String hash = sha256(target);
            jdbc.update("""
                    INSERT INTO evidence (
                        id, batch_id, uploaded_by, object_key, original_name,
                        content_type, file_size, sha256
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, evidenceId, batchId, user.id(), relative, original,
                    file.getContentType(), Files.size(target), hash);
            return new TraceModels.EvidenceView(
                    evidenceId, original, file.getContentType(), Files.size(target), hash,
                    java.time.LocalDateTime.now());
        } catch (Exception ex) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "附件上传失败：" + ex.getMessage());
        }
    }

    public DownloadedEvidence download(String evidenceId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM evidence WHERE id=?", evidenceId);
        if (rows.isEmpty()) {
            throw ApiException.notFound("附件不存在");
        }
        Map<String, Object> row = rows.get(0);
        CurrentUser user = security.currentUser();
        traces.requireBatchAccess(String.valueOf(row.get("batch_id")), user);
        Path path = uploadRoot.resolve(String.valueOf(row.get("object_key"))).normalize();
        if (!path.startsWith(uploadRoot) || !Files.exists(path)) {
            throw ApiException.notFound("附件文件不存在");
        }
        Resource resource = new FileSystemResource(path);
        return new DownloadedEvidence(
                resource,
                String.valueOf(row.get("original_name")),
                row.get("content_type") == null ? "application/octet-stream" : String.valueOf(row.get("content_type")),
                ((Number) row.get("file_size")).longValue()
        );
    }

    private String sha256(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(file);
             DigestInputStream stream = new DigestInputStream(input, digest)) {
            stream.transferTo(java.io.OutputStream.nullOutputStream());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    public record DownloadedEvidence(Resource resource, String filename, String contentType, long size) {
    }
}
