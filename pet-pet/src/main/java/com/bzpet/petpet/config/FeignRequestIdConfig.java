package com.bzpet.petpet.config;

import com.bzpet.common.constants.Constant;
import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 调用时把当前请求的 requestId 透传到被调服务。
 * <p>
 * MDC 是本线程的，Feign 发出的是新请求，不显式带上就会丢，
 * 导致 pet-pet -> pet-user 的链路断开。
 */
@Configuration
public class FeignRequestIdConfig {

    @Bean
    public RequestInterceptor requestIdInterceptor() {
        return template -> {
            String requestId = MDC.get(Constant.REQUEST_ID_HEADER);
            if (requestId != null && !requestId.isEmpty()) {
                template.header(Constant.REQUEST_ID_HEADER, requestId);
            }
        };
    }
}
