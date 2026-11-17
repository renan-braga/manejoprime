package br.com.curralzap.backend.whatsapp.application;

import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStatus;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record WhatsappSupportMessageQuery(
        int limit,
        int offset,
        String search,
        WhatsappProcessingStatus status
) {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    public static WhatsappSupportMessageQuery fromApi(
            String limit,
            String offset,
            String search,
            String status
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        Integer parsedLimit = parseLimit(limit, fieldErrors);
        Integer parsedOffset = parseOffset(offset, fieldErrors);
        WhatsappProcessingStatus parsedStatus = parseStatus(status, fieldErrors);

        if (!fieldErrors.isEmpty()) {
            throw new InvalidWhatsappSupportMessageQueryException(fieldErrors);
        }

        return new WhatsappSupportMessageQuery(
                parsedLimit,
                parsedOffset,
                normalizeText(search),
                parsedStatus
        );
    }

    private static Integer parseLimit(String limit, Map<String, String> fieldErrors) {
        if (limit == null || limit.isBlank()) {
            return DEFAULT_LIMIT;
        }

        try {
            int parsed = Integer.parseInt(limit.trim());
            if (parsed <= 0 || parsed > MAX_LIMIT) {
                fieldErrors.put("limit", "Limit must be between 1 and 100.");
                return DEFAULT_LIMIT;
            }
            return parsed;
        } catch (NumberFormatException exception) {
            fieldErrors.put("limit", "Limit must be a whole number.");
            return DEFAULT_LIMIT;
        }
    }

    private static Integer parseOffset(String offset, Map<String, String> fieldErrors) {
        if (offset == null || offset.isBlank()) {
            return 0;
        }

        try {
            int parsed = Integer.parseInt(offset.trim());
            if (parsed < 0) {
                fieldErrors.put("offset", "Offset must be zero or greater.");
                return 0;
            }
            return parsed;
        } catch (NumberFormatException exception) {
            fieldErrors.put("offset", "Offset must be a whole number.");
            return 0;
        }
    }

    private static WhatsappProcessingStatus parseStatus(String status, Map<String, String> fieldErrors) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return WhatsappProcessingStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            fieldErrors.put("status", "Status must be one of: " + supportedStatuses() + ".");
            return null;
        }
    }

    private static String supportedStatuses() {
        return Stream.of(WhatsappProcessingStatus.values())
                .map(value -> value.name().toLowerCase())
                .collect(Collectors.joining(", "));
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
