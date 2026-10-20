
package com.qrservice.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QrFilterConfig {

    @Bean
    public FilterRegistrationBean<QrApiKeyFilter> qrApiKeyFilterRegistration(
            QrApiKeyFilter qrApiKeyFilter) {

        FilterRegistrationBean<QrApiKeyFilter> registration =
                new FilterRegistrationBean<>();

        registration.setFilter(qrApiKeyFilter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);

        return registration;
    }
}
