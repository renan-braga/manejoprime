package br.com.curralzap.backend.shared.api;

import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.auth.application.AuthTokenParser;
import br.com.curralzap.backend.auth.application.AuthTokenType;
import br.com.curralzap.backend.auth.application.AuthenticationRequiredException;
import br.com.curralzap.backend.auth.application.ExpiredAuthTokenException;
import br.com.curralzap.backend.auth.application.InvalidAuthTokenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.cors.CorsUtils;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AccessTokenAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_TOKEN_CLAIMS_ATTRIBUTE = "curralzap.authTokenClaims";

    private final AuthTokenParser authTokenParser;
    private final HandlerExceptionResolver handlerExceptionResolver;

    public AccessTokenAuthenticationFilter(
            AuthTokenParser authTokenParser,
            @Qualifier("handlerExceptionResolver")
            HandlerExceptionResolver handlerExceptionResolver
    ) {
        this.authTokenParser = authTokenParser;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }

        String requestUri = request.getRequestURI();
        return !"/api/v1/session/me".equals(requestUri)
                && !requestUri.startsWith("/api/v1/dashboard")
                && !requestUri.startsWith("/api/v1/animals")
                && !requestUri.startsWith("/api/v1/feedings")
                && !requestUri.startsWith("/api/v1/lots")
                && !requestUri.startsWith("/api/v1/occurrences")
                && !requestUri.startsWith("/api/v1/settings")
                && !requestUri.startsWith("/api/v1/medications")
                && !requestUri.startsWith("/api/v1/vaccines")
                && !requestUri.startsWith("/api/v1/weighings")
                && !requestUri.startsWith("/api/v1/users")
                && !requestUri.startsWith("/api/v1/whatsapp/messages")
                && !requestUri.startsWith("/api/v1/offers")
                && !requestUri.startsWith("/api/v1/invites")
                && !requestUri.startsWith("/api/v1/members")
                && !requestUri.startsWith("/api/v1/billing/checkout")
                && !requestUri.startsWith("/api/v1/billing/customer")
                && !"/api/v1/profile".equals(requestUri)
                && !requestUri.startsWith("/api/v1/my-farms")
                && !requestUri.startsWith("/api/v1/auth/switch-farm");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");
            if (header == null || header.isBlank() || !header.startsWith("Bearer ")) {
                throw new AuthenticationRequiredException();
            }

            String token = header.substring("Bearer ".length()).trim();
            if (token.isEmpty()) {
                throw new AuthenticationRequiredException();
            }

            AuthTokenClaims claims = authTokenParser.verify(token, AuthTokenType.ACCESS, OffsetDateTime.now(ZoneOffset.UTC));
            request.setAttribute(AUTH_TOKEN_CLAIMS_ATTRIBUTE, claims);
            filterChain.doFilter(request, response);
        } catch (AuthenticationRequiredException | InvalidAuthTokenException | ExpiredAuthTokenException exception) {
            handlerExceptionResolver.resolveException(request, response, null, exception);
        }
    }
}
