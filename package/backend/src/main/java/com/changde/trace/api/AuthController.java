package com.changde.trace.api;

import com.changde.trace.security.JwtService;
import com.changde.trace.security.CurrentUser;
import com.changde.trace.security.SecuritySupport;
import com.changde.trace.security.UserDirectoryService;
import com.changde.trace.security.AccountService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserDirectoryService users;
    private final JwtService jwtService;
    private final SecuritySupport security;
    private final AccountService accounts;

    public AuthController(
            UserDirectoryService users,
            JwtService jwtService,
            SecuritySupport security,
            AccountService accounts
    ) {
        this.users = users;
        this.jwtService = jwtService;
        this.security = security;
        this.accounts = accounts;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody ApiModels.LoginRequest request) {
        CurrentUser user = users.authenticate(request.username(), request.password());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("token", jwtService.issue(user));
        response.put("user", user);
        return response;
    }

    @GetMapping("/me")
    public CurrentUser me() {
        return security.currentUser();
    }

    @PostMapping("/register")
    public Map<String, Object> register(
            @Valid @RequestBody ApiModels.EnterpriseRegisterRequest request
    ) {
        return accounts.registerEnterprise(request);
    }

    @PostMapping("/change-password")
    public Map<String, Object> changePassword(
            @Valid @RequestBody ApiModels.ChangePasswordRequest request
    ) {
        accounts.changePassword(request);
        return Map.of("message", "密码修改成功");
    }
}
