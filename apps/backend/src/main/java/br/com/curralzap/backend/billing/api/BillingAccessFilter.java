package br.com.curralzap.backend.billing.api;

import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.billing.application.BillingAccessDeniedException;
import br.com.curralzap.backend.billing.application.BillingAccessPolicy;
import br.com.curralzap.backend.shared.api.AccessTokenAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Set;

@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
class BillingAccessFilter extends OncePerRequestFilter {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final BillingAccessPolicy billingAccessPolicy;
    private final HandlerExceptionResolver handlerExceptionResolver;

    BillingAccessFilter(
            BillingAccessPolicy billingAccessPolicy,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver
    ) {
        this.billingAccessPolicy = billingAccessPolicy;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return CorsUtils.isPreFlightRequest(request)
                || SAFE_METHODS.contains(request.getMethod())
                || !isProtectedMutationPath(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            AuthTokenClaims claims = (AuthTokenClaims) request.getAttribute(AccessTokenAuthenticationFilter.AUTH_TOKEN_CLAIMS_ATTRIBUTE);
            if (claims != null) {
                billingAccessPolicy.requireWriteAllowed(claims.farmId());
            }
            filterChain.doFilter(request, response);
        } catch (BillingAccessDeniedException exception) {
            handlerExceptionResolver.resolveException(request, response, null, exception);
        }
    }

    private boolean isProtectedMutationPath(String requestUri) {
        return requestUri.startsWith("/api/v1/animals")
                || requestUri.startsWith("/api/v1/feedings")
                || requestUri.startsWith("/api/v1/lots")
                || requestUri.startsWith("/api/v1/occurrences")
                || requestUri.startsWith("/api/v1/settings")
                || requestUri.startsWith("/api/v1/medications")
                || requestUri.startsWith("/api/v1/vaccines")
                || requestUri.startsWith("/api/v1/weighings")
                || requestUri.startsWith("/api/v1/users")
                || requestUri.startsWith("/api/v1/whatsapp/messages")
                || requestUri.startsWith("/api/v1/offers")
                || requestUri.startsWith("/api/v1/invites")
                || requestUri.startsWith("/api/v1/members")
                || "/api/v1/profile".equals(requestUri);
    }
}
