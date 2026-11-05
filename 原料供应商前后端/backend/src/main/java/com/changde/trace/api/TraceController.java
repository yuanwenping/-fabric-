package com.changde.trace.api;

import com.changde.trace.security.CurrentUser;
import com.changde.trace.security.SecuritySupport;
import com.changde.trace.trace.BusinessRules;
import com.changde.trace.trace.ChainService;
import com.changde.trace.trace.EvidenceService;
import com.changde.trace.trace.TraceModels;
import com.changde.trace.trace.TraceService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TraceController {
    private final TraceService traces;
    private final EvidenceService evidence;
    private final ChainService chain;
    private final SecuritySupport security;

    public TraceController(
            TraceService traces,
            EvidenceService evidence,
            ChainService chain,
            SecuritySupport security
    ) {
        this.traces = traces;
        this.evidence = evidence;
        this.chain = chain;
        this.security = security;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        return traces.dashboard();
    }

    @GetMapping("/batches")
    public ApiModels.PageResponse<TraceModels.BatchView> batches(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return traces.listBatches(keyword, status, page, size);
    }

    @GetMapping("/batches/{batchId}")
    public TraceModels.BatchDetail batch(@PathVariable String batchId) {
        return traces.batchDetail(batchId);
    }

    @PostMapping("/batches")
    public TraceModels.BatchView createBatch(@Valid @RequestBody ApiModels.BatchCreateRequest request) {
        return traces.createBatch(request);
    }

    @PostMapping("/batches/{batchId}/events")
    public TraceModels.EventView appendEvent(
            @PathVariable String batchId,
            @Valid @RequestBody ApiModels.EventRequest request
    ) {
        return traces.appendEvent(batchId, request);
    }

    @PostMapping("/events/{eventId}/retry")
    public TraceModels.EventView retryEvent(@PathVariable String eventId) {
        return traces.retryEvent(eventId);
    }

    @GetMapping("/submissions/mine")
    public List<Map<String, Object>> mySubmissions() {
        return traces.mySubmissions();
    }

    @GetMapping("/reviews/pending")
    public List<Map<String, Object>> pendingReviews() {
        return traces.pendingReviews();
    }

    @GetMapping("/events/allowed")
    public List<String> allowedEvents() {
        return BusinessRules.allowedEventsFor(security.currentUser()).stream().toList();
    }

    @PostMapping("/reviews/{eventId}/approve")
    public TraceModels.EventView approveEvent(
            @PathVariable String eventId,
            @RequestBody(required = false) ApiModels.ReviewRequest request
    ) {
        return traces.approveEvent(eventId, request == null ? null : request.comment());
    }

    @PostMapping("/reviews/{eventId}/reject")
    public TraceModels.EventView rejectEvent(
            @PathVariable String eventId,
            @RequestBody ApiModels.ReviewRequest request
    ) {
        return traces.rejectEvent(eventId, request.comment());
    }

    @GetMapping("/batches/{batchId}/verify")
    public Map<String, Object> verify(@PathVariable String batchId) {
        return traces.verifyBatch(batchId);
    }

    @PostMapping(value = "/batches/{batchId}/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TraceModels.EvidenceView uploadEvidence(
            @PathVariable String batchId,
            @RequestPart("file") MultipartFile file
    ) {
        return evidence.upload(batchId, file);
    }

    @GetMapping("/evidence/{evidenceId}")
    public ResponseEntity<Resource> downloadEvidence(@PathVariable String evidenceId) {
        EvidenceService.DownloadedEvidence file = evidence.download(evidenceId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.filename(), StandardCharsets.UTF_8).build().toString())
                .body(file.resource());
    }

    @GetMapping("/system/chain")
    public Map<String, Object> chainStatus() {
        CurrentUser user = security.currentUser();
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("channel", "tracechannel");
        status.put("chaincode", "tracecc");
        status.put("identity", user.orgType());
        status.put("state", chain.status());
        return status;
    }

    @GetMapping("/directory/organizations")
    public List<Map<String, Object>> organizations() {
        return traces.activeOrganizations();
    }
}
