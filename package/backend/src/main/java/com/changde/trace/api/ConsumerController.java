package com.changde.trace.api;

import com.changde.trace.security.ConsumerPrincipal;
import com.changde.trace.security.ConsumerService;
import com.changde.trace.security.JwtService;
import com.changde.trace.security.SecuritySupport;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/consumer")
public class ConsumerController {
    private final ConsumerService consumers;
    private final JwtService jwtService;
    private final SecuritySupport security;

    public ConsumerController(
            ConsumerService consumers,
            JwtService jwtService,
            SecuritySupport security
    ) {
        this.consumers = consumers;
        this.jwtService = jwtService;
        this.security = security;
    }

    @PostMapping("/register")
    public Map<String, Object> register(
            @Valid @RequestBody ApiModels.ConsumerRegisterRequest request
    ) {
        return consumers.register(request);
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody ApiModels.LoginRequest request) {
        ConsumerPrincipal principal = consumers.authenticate(request.username(), request.password());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("token", jwtService.issueConsumer(principal.id(), principal.phone()));
        response.put("user", principal);
        return response;
    }

    @GetMapping("/profile")
    public Map<String, Object> profile() {
        return consumers.profile();
    }

    @PutMapping("/profile")
    public Map<String, Object> updateProfile(
            @Valid @RequestBody ApiModels.ConsumerProfileRequest request
    ) {
        return consumers.updateProfile(request);
    }

    @PostMapping("/change-password")
    public Map<String, Object> changePassword(
            @Valid @RequestBody ApiModels.ConsumerPasswordRequest request
    ) {
        consumers.changePassword(request);
        return Map.of("message", "密码修改成功");
    }

    @GetMapping("/history")
    public List<Map<String, Object>> history() {
        return consumers.history();
    }

    @PostMapping("/history")
    public Map<String, Object> recordHistory(
            @Valid @RequestBody ApiModels.TraceCodeRequest request
    ) {
        consumers.recordHistory(request.traceCode());
        return Map.of("message", "浏览记录已保存");
    }

    @GetMapping("/favorites")
    public List<Map<String, Object>> favorites() {
        return consumers.favorites();
    }

    @PostMapping("/favorites/toggle")
    public Map<String, Object> toggleFavorite(
            @Valid @RequestBody ApiModels.TraceCodeRequest request
    ) {
        return consumers.toggleFavorite(request.traceCode());
    }
}
