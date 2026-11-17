package br.com.curralzap.backend.animals.application;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record AnimalListQuery(
        int limit,
        int offset,
        String search,
        String statusDatabaseValue,
        UUID lotId,
        String sexDatabaseValue,
        String breed
) {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    public static AnimalListQuery fromApi(
            String limit,
            String offset,
            String search,
            String status,
            String lot,
            String sexo,
            String raca
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        Integer parsedLimit = parseLimit(limit, fieldErrors);
        Integer parsedOffset = parseOffset(offset, fieldErrors);
        UUID parsedLotId = parseLotId(lot, fieldErrors);
        String parsedStatus = parseStatus(status, fieldErrors);
        String parsedSex = parseSex(sexo, fieldErrors);

        if (!fieldErrors.isEmpty()) {
            throw new InvalidAnimalQueryException(fieldErrors);
        }

        return new AnimalListQuery(
                parsedLimit,
                parsedOffset,
                normalizeText(search),
                parsedStatus,
                parsedLotId,
                parsedSex,
                normalizeText(raca)
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

    private static UUID parseLotId(String lot, Map<String, String> fieldErrors) {
        if (lot == null || lot.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(lot.trim());
        } catch (IllegalArgumentException exception) {
            fieldErrors.put("lot", "Lot must be a valid UUID.");
            return null;
        }
    }

    private static String parseStatus(String status, Map<String, String> fieldErrors) {
        if (status == null || status.isBlank()) {
            return null;
        }

        return switch (status.trim().toLowerCase()) {
            case "active", "ativo" -> "ativo";
            case "sold", "vendido" -> "vendido";
            case "dead", "morto" -> "morto";
            case "transferred", "transferido" -> "transferido";
            default -> {
                fieldErrors.put("status", "Status must be active, sold, dead, or transferred.");
                yield null;
            }
        };
    }

    private static String parseSex(String sexo, Map<String, String> fieldErrors) {
        if (sexo == null || sexo.isBlank()) {
            return null;
        }

        return switch (sexo.trim().toLowerCase()) {
            case "male", "macho" -> "macho";
            case "female", "femea" -> "femea";
            default -> {
                fieldErrors.put("sexo", "Sexo must be male or female.");
                yield null;
            }
        };
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
