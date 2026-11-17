package br.com.curralzap.backend.shared.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ApiCorsConfiguration implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    ApiCorsConfiguration(
            @Value("${curralzap.api.cors.allowed-origins:}") String allowedOrigins
    ) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (allowedOrigins.length == 0) {
            return;
        }

        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders(RequestIdFilter.REQUEST_ID_HEADER);
    }
}
