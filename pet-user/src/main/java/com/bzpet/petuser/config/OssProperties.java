package com.bzpet.petuser.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云 OSS 配置（配置项来自 Nacos 的 aliyun.oss.*）
 */
@Data
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssProperties {

    @Schema(description = "OSS 访问域名，如 oss-cn-hangzhou.aliyuncs.com")
    private String endpoint;

    @Schema(description = "AccessKey ID")
    private String accessKeyId;

    @Schema(description = "AccessKey Secret")
    private String accessKeySecret;

    @Schema(description = "Bucket 名称")
    private String bucketName;

    @Schema(description = "访问域名（含协议），如 https://your-bucket.oss-cn-hangzhou.aliyuncs.com")
    private String domain;
}
