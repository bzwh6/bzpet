package com.bzpet.petgetway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 网关统一生成 / 透传请求链路 id（requestId），写入请求头传给下游服务。
 * <p>
 * 网关是全部请求的统一入口，在这里生成最合适；下游服务只需读取，不再重复生成。
 * 网关是响应式应用且不依赖 pet-common，这里直接使用同名的请求头常量。
 */
@Slf4j
@Component
public class RequestIdGlobalFilter implements GlobalFilter, Ordered {

    private static final String REQUEST_ID_HEADER = "requestId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        // 上游若已带（如前端传入）则复用，否则生成
        String requestId = request.getHeaders().getFirst(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString().replace("-", "");
        }
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(REQUEST_ID_HEADER, requestId)
                .build();
        // 同时回写到响应头，方便前端在报错时提供给后端排查
        exchange.getResponse().getHeaders().set(REQUEST_ID_HEADER, requestId);
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        // 优先级最高，保证在登录校验（LoginFilter，order=0）之前生成
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
