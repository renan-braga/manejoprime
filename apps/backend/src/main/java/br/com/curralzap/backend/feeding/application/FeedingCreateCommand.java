package br.com.curralzap.backend.feeding.application;

import br.com.curralzap.backend.feeding.domain.FeedingType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record FeedingCreateCommand(
        UUID animalId,
        UUID lotId,
        FeedingType feedType,
        String description,
        BigDecimal quantityKg,
        BigDecimal costPerKg,
        LocalDate startDate,
        LocalDate endDate
) {
    public static FeedingCreateCommand fromApi(
            String animalId,
            String lotId,
            String feedType,
            String description,
            String quantityKg,
            String costPerKg,
            String startDate,
            String endDate
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        UUID parsedAnimalId = parseOptionalUuid(animalId);
        UUID parsedLotId = parseOptionalUuid(lotId);
        validateTarget(animalId, lotId, parsedAnimalId, parsedLotId, fieldErrors);
        FeedingType parsedFeedType = parseFeedType(feedType, fieldErrors);
        String parsedDescription = parseDescription(description, fieldErrors);
        BigDecimal parsedQuantityKg = parsePositiveDecimal(
                quantityKg,
                "quantityKg",
                "Quantity is required.",
                "Quantity must be greater than zero.",
                fieldErrors
        );
        BigDecimal parsedCostPerKg = parseOptionalDecimal(
                costPerKg,
                "costPerKg",
                "Cost per kg must be zero or greater.",
                fieldErrors
        );
        LocalDate parsedStartDate = parseRequiredDate(startDate, "startDate", "Start date is required.", fieldErrors);
        LocalDate parsedEndDate = parseOptionalDate(endDate, "endDate", fieldErrors);

        if (parsedStartDate != null && parsedEndDate != null && parsedEndDate.isBefore(parsedStartDate)) {
            fieldErrors.put("endDate", "End date must be on or after start date.");
        }

        if (!fieldErrors.isEmpty()) {
            throw new InvalidFeedingMutationException(fieldErrors);
        }

        return new FeedingCreateCommand(
                parsedAnimalId,
                parsedLotId,
                parsedFeedType,
                parsedDescription,
                parsedQuantityKg,
                parsedCostPerKg,
                parsedStartDate,
                parsedEndDate
        );
    }

    public boolean animalTarget() {
        return animalId != null;
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

    private static FeedingType parseFeedType(String feedType, Map<String, String> fieldErrors) {
        if (feedType == null || feedType.isBlank()) {
            fieldErrors.put("feedType", "Feed type is required.");
            return null;
        }

        try {
            return FeedingType.fromDatabaseValue(feedType.trim());
        } catch (IllegalArgumentException exception) {
            fieldErrors.put("feedType", "Feed type must be pasto, racao, silagem, suplemento, sal_mineral, or misto.");
            return null;
        }
    }

    private static String parseDescription(String description, Map<String, String> fieldErrors) {
        if (description == null || description.isBlank()) {
            return null;
        }

        String normalized = description.trim();
        if (normalized.length() > 1000) {
            fieldErrors.put("description", "Description must have at most 1000 characters.");
            return null;
        }
        return normalized;
    }

    private static BigDecimal parsePositiveDecimal(
            String value,
            String field,
            String requiredMessage,
            String nonPositiveMessage,
            Map<String, String> fieldErrors
    ) {
        if (value == null || value.isBlank()) {
            fieldErrors.put(field, requiredMessage);
            return null;
        }

        try {
            BigDecimal parsed = normalizeMoneyLikeValue(value.trim());
            if (parsed.signum() <= 0) {
                fieldErrors.put(field, nonPositiveMessage);
                return null;
            }
            return parsed;
        } catch (ArithmeticException | NumberFormatException exception) {
            fieldErrors.put(field, "Quantity must be a valid number with at most 2 decimal places.");
            return null;
        }
    }

    private static BigDecimal parseOptionalDecimal(
            String value,
            String field,
            String negativeMessage,
            Map<String, String> fieldErrors
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            BigDecimal parsed = normalizeMoneyLikeValue(value.trim());
            if (parsed.signum() < 0) {
                fieldErrors.put(field, negativeMessage);
                return null;
            }
            return parsed;
        } catch (ArithmeticException | NumberFormatException exception) {
            fieldErrors.put(field, "Cost per kg must be a valid number with at most 2 decimal places.");
            return null;
        }
    }

    private static BigDecimal normalizeMoneyLikeValue(String value) {
        BigDecimal parsed = new BigDecimal(value);
        BigDecimal normalized = parsed.stripTrailingZeros();
        int scale = Math.max(normalized.scale(), 0);
        if (scale > 2) {
            throw new ArithmeticException("Too many decimals");
        }
        if (parsed.compareTo(new BigDecimal("99999999.99")) > 0) {
            throw new ArithmeticException("Exceeds maximum");
        }
        return parsed.setScale(2, RoundingMode.UNNECESSARY);
    }

    private static LocalDate parseRequiredDate(
            String value,
            String field,
            String requiredMessage,
            Map<String, String> fieldErrors
    ) {
        if (value == null || value.isBlank()) {
            fieldErrors.put(field, requiredMessage);
            return null;
        }

        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException exception) {
            fieldErrors.put(field, capitalize(field) + " must be a valid ISO date.");
            return null;
        }
    }

    private static LocalDate parseOptionalDate(String value, String field, Map<String, String> fieldErrors) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException exception) {
            fieldErrors.put(field, capitalize(field) + " must be a valid ISO date.");
            return null;
        }
    }

    private static String capitalize(String field) {
        return Character.toUpperCase(field.charAt(0)) + field.substring(1);
    }
}
