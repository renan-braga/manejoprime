package br.com.curralzap.backend.lots.application;

import br.com.curralzap.backend.lots.domain.LotPurpose;

import java.util.LinkedHashMap;
import java.util.Map;

public record LotListQuery(
        int limit,
        int offset,
        String search,
        String purposeDatabaseValue
) {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    public static LotListQuery fromApi(
            String limit,
            String offset,
            String search,
            String purpose
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        Integer parsedLimit = parseLimit(limit, fieldErrors);
        Integer parsedOffset = parseOffset(offset, fieldErrors);
        String parsedPurpose = parsePurpose(purpose, fieldErrors);

        if (!fieldErrors.isEmpty()) {
            throw new InvalidLotQueryException(fieldErrors);
        }

        return new LotListQuery(
                parsedLimit,
                parsedOffset,
                normalizeText(search),
                parsedPurpose
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

    private static String parsePurpose(String purpose, Map<String, String> fieldErrors) {
        if (purpose == null || purpose.isBlank()) {
            return null;
        }

        try {
            return LotPurpose.fromApiValue(purpose.trim()).databaseValue();
        } catch (IllegalArgumentException exception) {
            fieldErrors.put("purpose", "Purpose must be breeding, raising, fattening, or reproduction.");
            return null;
        }
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
