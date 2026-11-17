package br.com.curralzap.backend.shared.api;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import br.com.curralzap.backend.shared.infrastructure.logging.HttpRequestLogEmitter;
import jakarta.servlet.ServletException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdFilterTest {

    @Test
    void logsRequestCompletionWithRequestIdInMdcAndClearsContextAfterward() throws ServletException, IOException {
        RequestIdFilter filter = new RequestIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/magic-link/request");
        request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, "req-test-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> {
            assertThat(MDC.get("requestId")).isEqualTo("req-test-123");
            ((MockHttpServletResponse) servletResponse).setStatus(202);
        };
        ListAppender<ILoggingEvent> appender = attachAppender(HttpRequestLogEmitter.class);

        filter.doFilter(request, response, chain);

        assertThat(request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE)).isEqualTo("req-test-123");
        assertThat(response.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).isEqualTo("req-test-123");
        assertThat(MDC.get("requestId")).isNull();
        assertThat(appender.list)
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getFormattedMessage())
                            .contains("HTTP request completed")
                            .contains("method=POST")
                            .contains("path=/api/v1/auth/magic-link/request")
                            .contains("status=202");
                    assertThat(event.getMDCPropertyMap()).containsEntry("requestId", "req-test-123");
                });
        detachAppender(HttpRequestLogEmitter.class, appender);
    }

    private static ListAppender<ILoggingEvent> attachAppender(Class<?> type) {
        Logger logger = (Logger) LoggerFactory.getLogger(type);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private static void detachAppender(Class<?> type, ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger(type);
        logger.detachAppender(appender);
    }
}
