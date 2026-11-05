package com.changde.trace.security;

public record ConsumerPrincipal(
        Long id,
        String phone,
        String nickname,
        boolean profileCompleted
) {
}
