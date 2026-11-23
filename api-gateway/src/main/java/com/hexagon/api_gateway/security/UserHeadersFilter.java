package com.hexagon.api_gateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Removes any X-User-Id / X-User-Role sent by the client and, for authenticated requests,
 * adds the values taken from the validated token. The services trust only these headers.
 */
@Component
public class UserHeadersFilter implements GlobalFilter, Ordered {

    public static final String USER_ID = "X-User-Id";
    public static final String USER_ROLE = "X-User-Role";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .map(auth -> rewrite(exchange, auth.getToken().getSubject(), auth.getToken().getClaimAsString("role")))
                .switchIfEmpty(Mono.fromSupplier(() -> rewrite(exchange, null, null)))
                .flatMap(chain::filter);
    }

    private ServerWebExchange rewrite(ServerWebExchange exchange, String userId, String role) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(USER_ID);
                    headers.remove(USER_ROLE);
                    if (userId != null && role != null) {
                        headers.set(USER_ID, userId);
                        headers.set(USER_ROLE, role);
                    }
                })
                .build();
        return exchange.mutate().request(request).build();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
