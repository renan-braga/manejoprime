package br.com.curralzap.backend.medications.application;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record MedicationListQuery(
        int limit,
        int offset,
        UUID animalId
) {
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    public static MedicationListQuery fromApi(String limit, String offset, String animalId) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        Integer parsedLimit = parseLimit(limit, fieldErrors);
        Integer parsedOffset = parseOffset(offset, fieldErrors);
        UUID parsedAnimalId = parseRequiredAnimalId(animalId, fieldErrors);

        if (!fieldErrors.isEmpty()) {
            throw new InvalidMedicationQueryException(fieldErrors);
        }

        return new MedicationListQuery(parsedLimit, parsedOffset, parsedAnimalId);
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

    private static UUID parseRequiredAnimalId(String animalId, Map<String, String> fieldErrors) {
        if (animalId == null || animalId.isBlank()) {
            fieldErrors.put("animalId", "Animal id is required.");
            return null;
        }

        try {
            return UUID.fromString(animalId.trim());
        } catch (IllegalArgumentException exception) {
            fieldErrors.put("animalId", "Animal id must be a valid UUID.");
            return null;
        }
    }
}
