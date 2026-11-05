package com.changde.trace.security;

import java.util.Set;

public record CurrentUser(
        Long id,
        String username,
        String displayName,
        Long orgId,
        String orgCode,
        String orgName,
        String orgType,
        Set<String> roles
) {
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean isRegulator() {
        return "REGULATOR".equals(orgType) || hasRole("REGULATOR");
    }

    public boolean isAdmin() {
        return "ADMIN".equals(orgType) || hasRole("PLATFORM_ADMIN");
    }
}
