package com.changde.trace.api;

import com.changde.trace.security.AccountService;
import com.changde.trace.security.CurrentUser;
import com.changde.trace.security.SecuritySupport;
import jakarta.validation.Valid;
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
@RequestMapping("/api/org")
public class OrganizationUserController {
    private final AccountService accounts;
    private final SecuritySupport security;

    public OrganizationUserController(AccountService accounts, SecuritySupport security) {
        this.accounts = accounts;
        this.security = security;
    }

    @GetMapping("/users")
    public List<Map<String, Object>> users() {
        CurrentUser current = security.currentUser();
        return accounts.enterpriseUsers(current.orgId());
    }

    @GetMapping("/roles")
    public List<Map<String, Object>> roles() {
        return accounts.allowedRolesForCurrentOrganization();
    }

    @PostMapping("/users")
    public Map<String, Object> createUser(
            @Valid @RequestBody ApiModels.OrgUserCreateRequest request
    ) {
        return accounts.createEnterpriseUser(request);
    }

    @PutMapping("/users/{userId}/status")
    public Map<String, Object> setStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, Boolean> body
    ) {
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        accounts.setEnterpriseUserStatus(userId, enabled);
        return Map.of("id", userId, "enabled", enabled);
    }

    @PutMapping("/users/{userId}/password")
    public Map<String, Object> resetPassword(
            @PathVariable Long userId,
            @Valid @RequestBody ApiModels.PasswordResetRequest request
    ) {
        accounts.resetEnterprisePassword(userId, request.newPassword());
        return Map.of("id", userId, "message", "密码已重置");
    }
}
