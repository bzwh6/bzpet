package com.bzpet.common.autoConfig.swagger;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serializable;

@Data
@ConfigurationProperties(prefix = "bz.swagger")
public class SwaggerConfigProperties {
    private Boolean enable = false;
    private Boolean enableResponseWrap = false;

    public String packagePath;

    public String title;

    public String description;

    public String contactName;

    public String contactUrl;

    public String contactEmail;

    public String version;
}
