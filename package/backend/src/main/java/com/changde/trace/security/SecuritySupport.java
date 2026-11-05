package com.changde.trace.security;

import com.changde.trace.api.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecuritySupport {
    public CurrentUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser user)) {
            throw ApiException.forbidden("登录状态已失效");
        }
        return user;
    }

    public ConsumerPrincipal currentConsumer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof ConsumerPrincipal consumer)) {
            throw ApiException.forbidden("普通用户登录状态已失效");
        }
        return consumer;
    }
}
