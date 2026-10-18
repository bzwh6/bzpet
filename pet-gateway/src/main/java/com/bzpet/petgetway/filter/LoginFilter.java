package com.bzpet.petgetway.filter;


import com.bzpet.petjwt.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginFilter implements GlobalFilter, Ordered {
    private final StringRedisTemplate stringRedisTemplate;
    // 白名单路径（不需要登录）：登录、注册，以及首页用到的只读接口（列表 + 品种字典）
    private static final List<String> WHITE_LIST = List.of(
            "/user/login",
            "/user/register",
            "/pets/list",
            "/pets/breeds"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //判断是否是白名单路径
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String path = request.getURI().getPath();
        if (WHITE_LIST.contains(path)) {
            return chain.filter(exchange);
        }
        //从请求头里获取token
        String token = request.getHeaders().getFirst("Authorization");
        if (token == null || token.isEmpty()) {
            log.warn("token为空");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        //验证token
        try {
            Map<String, Object> map = JwtUtil.parseToken(token);
            //向下游传递用户id和签名
            log.info("解析出来的userId:{}",map.get("userId").toString());
            //从redis里获取用户id
            String userId = stringRedisTemplate.opsForValue().get("user:token:" + token);
            if (userId == null || userId.isEmpty()) {
                log.warn("token验证失败");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            }
            //向下游传递用户id和签名
            ServerHttpRequest httpRequest = request.mutate()
                    .header("userId", userId)
                    .build();
            //续上redis的过期时间
            stringRedisTemplate.expire("user:token:" + token, 3 * 3600, TimeUnit.SECONDS);
            return chain.filter(exchange.mutate().request(httpRequest).build());
        } catch (Exception e) {
            log.warn("token验证失败");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
