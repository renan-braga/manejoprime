package br.com.curralzap.backend.shared.infrastructure.logging;

import org.slf4j.MDC;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class LogContextScope implements AutoCloseable {

    private final Map<String, String> previousValues;
    private final Iterable<String> keys;

    private LogContextScope(Map<String, String> previousValues, Iterable<String> keys) {
        this.previousValues = previousValues;
        this.keys = keys;
    }

    public static LogContextScope with(Map<String, String> values) {
        Map<String, String> previousValues = new LinkedHashMap<>();
        for (String key : values.keySet()) {
            previousValues.put(key, MDC.get(key));
        }

        values.forEach((key, value) -> {
            if (value == null || value.isBlank()) {
                MDC.remove(key);
                return;
            }
            MDC.put(key, value);
        });
        return new LogContextScope(previousValues, values.keySet());
    }

    @Override
    public void close() {
        for (String key : keys) {
            String previousValue = previousValues.get(key);
            if (Objects.isNull(previousValue) || previousValue.isBlank()) {
                MDC.remove(key);
                continue;
            }
            MDC.put(key, previousValue);
        }
    }
}
