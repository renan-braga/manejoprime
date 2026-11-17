package br.com.curralzap.backend.shared.api;

import br.com.curralzap.backend.shared.infrastructure.logging.CurralzapLogContext;
import br.com.curralzap.backend.shared.infrastructure.logging.HttpRequestLogEmitter;
import br.com.curralzap.backend.shared.infrastructure.logging.LogContextScope;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_ATTRIBUTE = "curralzap.requestId";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = "req-" + UUID.randomUUID();
        }

        long startedAt = System.nanoTime();
        try (LogContextScope ignored = CurralzapLogContext.withRequestId(requestId)) {
            request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
            response.setHeader(REQUEST_ID_HEADER, requestId);
            filterChain.doFilter(request, response);
            HttpRequestLogEmitter.logCompleted(
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    (System.nanoTime() - startedAt) / 1_000_000
            );
        }
    }
}
