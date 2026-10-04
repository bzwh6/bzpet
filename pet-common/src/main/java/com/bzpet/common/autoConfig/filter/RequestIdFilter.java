package com.bzpet.common.autoConfig.filter;

import com.bzpet.common.constants.Constant;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求链路 id 过滤器。
 * <p>
 * 为每次请求生成（或复用上游透传的）requestId，写入 MDC 与响应头。
 * 日志 pattern 中使用 %X{requestId} 即可自动带出该 id，从而按一次请求定位全部日志。
 * 请求结束务必清理 MDC，否则 Tomcat 线程复用会导致日志串号。
 */
public class RequestIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        // 优先使用上游（网关 / Feign）透传的 requestId，没有则自己生成
        String requestId = request.getHeader(Constant.REQUEST_ID_HEADER);
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put(Constant.REQUEST_ID_HEADER, requestId);
        response.setHeader(Constant.REQUEST_ID_HEADER, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(Constant.REQUEST_ID_HEADER);
        }
    }
}
