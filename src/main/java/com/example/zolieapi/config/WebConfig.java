package com.example.zolieapi.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(ZolieProperties.class)
public class WebConfig implements WebMvcConfigurer {

    public static final String API_PREFIX = "/api/v1";

    private static final String CONTROLLER_PACKAGE = "com.example.zolieapi.controller";

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(API_PREFIX, HandlerTypePredicate.forBasePackage(CONTROLLER_PACKAGE));
    }
}
