package com.xm.cryptoservice.config;

import jakarta.servlet.Filter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import static org.junit.jupiter.api.Assertions.*;

class RateLimittingConfigTest {

    RateLimittingConfig config = new RateLimittingConfig();

    @Test
    void rateLimitFilter_registrationBeanConfigured() {
        FilterRegistrationBean<Filter> bean = config.rateLimitFilter();

        assertNotNull(bean);
        assertTrue(bean.getFilter() instanceof Bucket4jFilter);
        assertTrue(bean.getUrlPatterns().contains("/cryptos/*"));
    }
}
