package com.hms.api_gateway.configs;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        return exchange.getPrincipal()
                .cast(JwtAuthenticationToken.class)
                .map(authentication -> {
                    Jwt jwt = authentication.getToken();

                    String userId = jwt.getSubject();
                    String userRole = jwt.getClaim("role");
                    String userEmail = jwt.getClaim("email");

                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                            .header("X-User-ID", userId)
                            .header("X-User-Role", userRole)
                            .header("X-User-Email", userEmail)
                            .build();

                    return exchange.mutate().request(mutatedRequest).build();
                })
                .switchIfEmpty(Mono.just(exchange))
                .flatMap(chain::filter);
    }

    @Override
    public int getOrder() {
        return -1;
    }
}