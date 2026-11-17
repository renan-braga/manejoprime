package br.com.curralzap.backend.ops.api;

import br.com.curralzap.backend.ops.application.OpsAuthenticationRequiredException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
class OpsApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String OPS_API_KEY_HEADER = "X-Ops-Api-Key";
    private static final String OPS_PATH_PREFIX = "/api/v1/ops/";

    private final String opsApiKey;
    private final HandlerExceptionResolver handlerExceptionResolver;

    OpsApiKeyAuthenticationFilter(
            @Value("${curralzap.ops.api-key}") String opsApiKey,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver
    ) {
        this.opsApiKey = opsApiKey;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }
        return !request.getRequestURI().startsWith(OPS_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String providedKey = request.getHeader(OPS_API_KEY_HEADER);
        if (providedKey == null || !providedKey.equals(opsApiKey)) {
            handlerExceptionResolver.resolveException(request, response, null, new OpsAuthenticationRequiredException());
            return;
        }
        filterChain.doFilter(request, response);
    }
}
