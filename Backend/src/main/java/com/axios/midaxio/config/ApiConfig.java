package com.axios.midaxio.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApiConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {

        configurer.addPathPrefix("/api/v1",
                HandlerTypePredicate.forBasePackage("com.axios.midaxio.controller.v1"));
        configurer.addPathPrefix("/api/v2",
                HandlerTypePredicate.forBasePackage("com.axios.midaxio.controller.v2"));
    }
}