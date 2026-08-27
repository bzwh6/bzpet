package com.bzpet.common.autoConfig.xxljob;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "xxl.job")
public class XxlJobConfigProperties {
    private Admin admin = new Admin();
    private Executor executor = new Executor();

    @Data
    public static class Admin{
        private String addresses;
    }

    @Data
    public static class Executor{
        private String name;
        private String address;
        private String ip;
        private int port;
        private String accessToken;
        private String logPath;
        private int logRetentionDays;
    }
}
