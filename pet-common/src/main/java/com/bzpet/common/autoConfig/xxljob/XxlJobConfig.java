package com.bzpet.common.autoConfig.xxljob;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(prefix = "xxl.job", name = "executor.name")
@EnableConfigurationProperties(XxlJobConfigProperties.class)
public class XxlJobConfig {
    @Bean
    public XxlJobSpringExecutor xxlJobSpringExecutor(XxlJobConfigProperties properties) {
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAppname(properties.getExecutor().getName());
        executor.setAdminAddresses(properties.getAdmin().getAddresses());
        executor.setPort(properties.getExecutor().getPort());
        executor.setAccessToken(properties.getExecutor().getAccessToken());
        executor.setLogPath(properties.getExecutor().getLogPath());
        executor.setLogRetentionDays(properties.getExecutor().getLogRetentionDays());
        return executor;
    }
}
