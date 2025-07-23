package com.xm.cryptoservice.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.servlet.*;

@Configuration
public class RateLimittingConfig {
    @Bean
    public FilterRegistrationBean<Filter> rateLimitFilter() {
        FilterRegistrationBean<Filter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new Bucket4jFilter());
        reg.addUrlPatterns("/cryptos/*");
        return reg;
    }
}
