package br.com.curralzap.backend.vaccines.application;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record VaccineListQuery(
        int limit,
        int offset,
        UUID animalId,
        UUID lotId
) {
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    public static VaccineListQuery fromApi(
            String limit,
            String offset,
            String animalId,
            String lotId
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        Integer parsedLimit = parseLimit(limit, fieldErrors);
        Integer parsedOffset = parseOffset(offset, fieldErrors);
        UUID parsedAnimalId = parseOptionalUuid(animalId);
        UUID parsedLotId = parseOptionalUuid(lotId);
        validateTarget(animalId, lotId, parsedAnimalId, parsedLotId, fieldErrors);

        if (!fieldErrors.isEmpty()) {
            throw new InvalidVaccineQueryException(fieldErrors);
        }

        return new VaccineListQuery(parsedLimit, parsedOffset, parsedAnimalId, parsedLotId);
    }

    public boolean animalTarget() {
        return animalId != null;
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

    private static UUID parseOptionalUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static void validateTarget(
            String animalId,
            String lotId,
            UUID parsedAnimalId,
            UUID parsedLotId,
            Map<String, String> fieldErrors
    ) {
        boolean animalProvided = animalId != null && !animalId.isBlank();
        boolean lotProvided = lotId != null && !lotId.isBlank();

        if (animalProvided == lotProvided) {
            fieldErrors.put("target", "Exactly one of animalId or lotId is required.");
            return;
        }

        if (animalProvided && parsedAnimalId == null) {
            fieldErrors.put("target", "Exactly one of animalId or lotId must be a valid UUID.");
        }

        if (lotProvided && parsedLotId == null) {
            fieldErrors.put("target", "Exactly one of animalId or lotId must be a valid UUID.");
        }
    }
}
