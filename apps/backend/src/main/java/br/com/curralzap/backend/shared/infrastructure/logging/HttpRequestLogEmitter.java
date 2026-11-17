package br.com.curralzap.backend.shared.infrastructure.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HttpRequestLogEmitter {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequestLogEmitter.class);

    private HttpRequestLogEmitter() {
    }

    public static void logCompleted(String method, String path, int status, long durationMs) {
        LOGGER.info(
                "HTTP request completed method={} path={} status={} durationMs={}",
                method,
                path,
                status,
                durationMs
        );
    }
}
