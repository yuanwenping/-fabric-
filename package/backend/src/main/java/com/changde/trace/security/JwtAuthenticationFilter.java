package com.changde.trace.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDirectoryService users;
    private final ConsumerService consumers;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserDirectoryService users,
            ConsumerService consumers
    ) {
        this.jwtService = jwtService;
        this.users = users;
        this.consumers = consumers;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            try {
                JwtService.TokenData token = jwtService.parse(authorization.substring(7));
                Object principal;
                List<SimpleGrantedAuthority> authorities;
                if ("CONSUMER".equals(token.userType())) {
                    principal = consumers.loadPrincipal(token.id());
                    authorities = List.of(new SimpleGrantedAuthority("ROLE_CONSUMER"));
                } else {
                    CurrentUser user = users.load(token.id());
                    principal = user;
                    authorities = user.roles().stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .toList();
                }
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
