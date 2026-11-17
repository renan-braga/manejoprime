package br.com.curralzap.backend.lots.application;

import br.com.curralzap.backend.lots.domain.LotPurpose;

import java.util.LinkedHashMap;
import java.util.Map;

public record LotUpsertInput(
        String name,
        LotPurpose purpose,
        String pastureArea
) {
    public static LotUpsertInput fromApi(String name, String purpose, String pastureArea) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        String parsedName = parseRequiredText(name, "name", "Name", 100, fieldErrors);
        LotPurpose parsedPurpose = parsePurpose(purpose, fieldErrors);
        String parsedPastureArea = parseOptionalText(pastureArea, "pastureArea", "Pasture area", 100, fieldErrors);

        if (!fieldErrors.isEmpty()) {
            throw new InvalidLotUpsertException(fieldErrors);
        }

        return new LotUpsertInput(parsedName, parsedPurpose, parsedPastureArea);
    }

    private static String parseRequiredText(
            String value,
            String field,
            String label,
            int maxLength,
            Map<String, String> fieldErrors
    ) {
        if (value == null || value.isBlank()) {
            fieldErrors.put(field, label + " is required.");
            return null;
        }

        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            fieldErrors.put(field, label + " must be " + maxLength + " characters or fewer.");
            return null;
        }

        return normalized;
    }

    private static String parseOptionalText(
            String value,
            String field,
            String label,
            int maxLength,
            Map<String, String> fieldErrors
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            fieldErrors.put(field, label + " must be " + maxLength + " characters or fewer.");
            return null;
        }

        return normalized;
    }

    private static LotPurpose parsePurpose(String purpose, Map<String, String> fieldErrors) {
        if (purpose == null || purpose.isBlank()) {
            fieldErrors.put("purpose", "Purpose is required.");
            return null;
        }

        try {
            return LotPurpose.fromApiValue(purpose.trim());
        } catch (IllegalArgumentException exception) {
            fieldErrors.put("purpose", "Purpose must be breeding, raising, fattening, or reproduction.");
            return null;
        }
    }
}
