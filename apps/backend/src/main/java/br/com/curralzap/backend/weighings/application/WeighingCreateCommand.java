package br.com.curralzap.backend.weighings.application;

import br.com.curralzap.backend.weighings.domain.WeighingType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record WeighingCreateCommand(
        UUID animalId,
        BigDecimal weightKg,
        LocalDate weighingDate,
        WeighingType type
) {
    public static WeighingCreateCommand fromApi(
            String animalId,
            String weightKg,
            String weighingDate,
            String type
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        UUID parsedAnimalId = parseAnimalId(animalId, fieldErrors);
        BigDecimal parsedWeightKg = parseWeightKg(weightKg, fieldErrors);
        LocalDate parsedWeighingDate = parseWeighingDate(weighingDate, fieldErrors);
        WeighingType parsedType = parseType(type, fieldErrors);

        if (!fieldErrors.isEmpty()) {
            throw new InvalidWeighingMutationException(fieldErrors);
        }

        return new WeighingCreateCommand(
                parsedAnimalId,
                parsedWeightKg,
                parsedWeighingDate,
                parsedType
        );
    }

    private static UUID parseAnimalId(String animalId, Map<String, String> fieldErrors) {
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

    private static BigDecimal parseWeightKg(String weightKg, Map<String, String> fieldErrors) {
        if (weightKg == null || weightKg.isBlank()) {
            fieldErrors.put("weightKg", "Weight is required.");
            return null;
        }

        try {
            BigDecimal parsed = new BigDecimal(weightKg.trim());
            BigDecimal normalized = parsed.stripTrailingZeros();
            int scale = Math.max(normalized.scale(), 0);
            if (scale > 2) {
                fieldErrors.put("weightKg", "Weight must have at most 2 decimal places.");
                return null;
            }
            if (parsed.signum() <= 0) {
                fieldErrors.put("weightKg", "Weight must be greater than zero.");
                return null;
            }
            if (parsed.compareTo(new BigDecimal("999999.99")) > 0) {
                fieldErrors.put("weightKg", "Weight exceeds the supported maximum.");
                return null;
            }
            return parsed.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException | NumberFormatException exception) {
            fieldErrors.put("weightKg", "Weight must be a valid number.");
            return null;
        }
    }

    private static LocalDate parseWeighingDate(String weighingDate, Map<String, String> fieldErrors) {
        if (weighingDate == null || weighingDate.isBlank()) {
            fieldErrors.put("weighingDate", "Weighing date is required.");
            return null;
        }

        try {
            return LocalDate.parse(weighingDate.trim());
        } catch (DateTimeParseException exception) {
            fieldErrors.put("weighingDate", "Weighing date must be a valid ISO date.");
            return null;
        }
    }

    private static WeighingType parseType(String type, Map<String, String> fieldErrors) {
        if (type == null || type.isBlank()) {
            fieldErrors.put("type", "Type is required.");
            return null;
        }

        try {
            return WeighingType.fromApiValue(type.trim());
        } catch (IllegalArgumentException exception) {
            fieldErrors.put("type", "Type must be routine, entry, exit, weaning, or slaughter.");
            return null;
        }
    }
}
