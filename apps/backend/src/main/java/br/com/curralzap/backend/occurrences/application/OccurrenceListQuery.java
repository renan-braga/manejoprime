package br.com.curralzap.backend.occurrences.application;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record OccurrenceListQuery(
        int limit,
        int offset,
        UUID animalId,
        String status,
        String severity
) {
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final Set<String> ALLOWED_STATUSES = Set.of("open", "in_follow_up", "resolved");
    private static final Set<String> ALLOWED_SEVERITIES = Set.of("low", "medium", "high", "critical");

    public static OccurrenceListQuery fromApi(
            String limit,
            String offset,
            String animalId,
            String status,
            String severity
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        Integer parsedLimit = parseLimit(limit, fieldErrors);
        Integer parsedOffset = parseOffset(offset, fieldErrors);
        UUID parsedAnimalId = parseOptionalAnimalId(animalId, fieldErrors);
        String parsedStatus = parseOptionalValue(status, "status", ALLOWED_STATUSES, fieldErrors);
        String parsedSeverity = parseOptionalValue(severity, "severity", ALLOWED_SEVERITIES, fieldErrors);

        if (!fieldErrors.isEmpty()) {
            throw new InvalidOccurrenceQueryException(fieldErrors);
        }

        return new OccurrenceListQuery(parsedLimit, parsedOffset, parsedAnimalId, parsedStatus, parsedSeverity);
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

    private static UUID parseOptionalAnimalId(String animalId, Map<String, String> fieldErrors) {
        if (animalId == null || animalId.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(animalId.trim());
        } catch (IllegalArgumentException exception) {
            fieldErrors.put("animalId", "Animal id must be a valid UUID.");
            return null;
        }
    }

    private static String parseOptionalValue(
            String value,
            String fieldName,
            Set<String> allowedValues,
            Map<String, String> fieldErrors
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().toLowerCase();
        if (!allowedValues.contains(normalized)) {
            fieldErrors.put(fieldName, fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1) + " is invalid.");
            return null;
        }

        return normalized;
    }
}
