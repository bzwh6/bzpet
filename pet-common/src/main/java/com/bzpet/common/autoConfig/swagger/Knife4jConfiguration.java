package com.bzpet.common.autoConfig.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "bz.swagger",name = "enable",havingValue = "true")
@EnableConfigurationProperties(SwaggerConfigProperties.class)
public class Knife4jConfiguration {

    @Resource
    private SwaggerConfigProperties swaggerConfigProperties;

    @Bean
    public OpenAPI openAPI(){
        return new OpenAPI()
                .info(new Info()
                        .title(swaggerConfigProperties.getTitle())
                        .description(swaggerConfigProperties.getDescription())
                        .version(swaggerConfigProperties.getVersion())
                        .contact(new Contact()
                                .name(swaggerConfigProperties.getContactName())
                                .email(swaggerConfigProperties.getContactEmail())
                                .url(swaggerConfigProperties.getContactUrl())));
    }
}
