package com.ecom.orders.configs;

import com.ecom.orders.auth.AuthInterceptor;
import com.ecom.orders.auth.InternalApiInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AuthInterceptor authInterceptor;
    private final InternalApiInterceptor internalApiInterceptor;

    public WebConfig(AuthInterceptor authInterceptor, InternalApiInterceptor internalApiInterceptor) {
        this.authInterceptor = authInterceptor;
        this.internalApiInterceptor = internalApiInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Customer and admin endpoints: user login token (admin endpoints also check the ADMIN role)
        registry.addInterceptor(authInterceptor).addPathPatterns("/api/v1/orders", "/api/v1/orders/**",
                "/api/v1/admin/orders", "/api/v1/admin/orders/**");
        // Service-to-service endpoints: shared internal token
        registry.addInterceptor(internalApiInterceptor).addPathPatterns("/internal/**");
    }
}
