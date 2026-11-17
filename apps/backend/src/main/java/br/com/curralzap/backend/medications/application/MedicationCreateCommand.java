package br.com.curralzap.backend.medications.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record MedicationCreateCommand(
        UUID animalId,
        UUID occurrenceId,
        String medicationName,
        String activeIngredient,
        String reason,
        String dosage,
        int withdrawalPeriodDays,
        LocalDate applicationDate,
        BigDecimal cost
) {
    public static MedicationCreateCommand fromApi(
            String animalId,
            String occurrenceId,
            String medicationName,
            String activeIngredient,
            String reason,
            String dosage,
            String withdrawalPeriodDays,
            String applicationDate,
            String cost
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        UUID parsedAnimalId = parseRequiredUuid(animalId, "animalId", "Animal id must be a valid UUID.", fieldErrors);
        UUID parsedOccurrenceId = parseOptionalUuid(occurrenceId, "occurrenceId", "Occurrence id must be a valid UUID.", fieldErrors);
        String parsedMedicationName = parseMedicationName(medicationName, fieldErrors);
        String parsedActiveIngredient = parseActiveIngredient(activeIngredient, fieldErrors);
        String parsedReason = normalizeOptionalText(reason);
        String parsedDosage = parseDosage(dosage, fieldErrors);
        Integer parsedWithdrawalPeriodDays = parseWithdrawalPeriodDays(withdrawalPeriodDays, fieldErrors);
        LocalDate parsedApplicationDate = parseApplicationDate(applicationDate, fieldErrors);
        BigDecimal parsedCost = parseCost(cost, fieldErrors);

        if (!fieldErrors.isEmpty()) {
            throw new InvalidMedicationMutationException(fieldErrors);
        }

        return new MedicationCreateCommand(
                parsedAnimalId,
                parsedOccurrenceId,
                parsedMedicationName,
                parsedActiveIngredient,
                parsedReason,
                parsedDosage,
                parsedWithdrawalPeriodDays,
                parsedApplicationDate,
                parsedCost
        );
    }

    private static UUID parseRequiredUuid(
            String value,
            String field,
            String invalidMessage,
            Map<String, String> fieldErrors
    ) {
        if (value == null || value.isBlank()) {
            fieldErrors.put(field, "Animal id is required.");
            return null;
        }

        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException exception) {
            fieldErrors.put(field, invalidMessage);
            return null;
        }
    }

    private static UUID parseOptionalUuid(
            String value,
            String field,
            String invalidMessage,
            Map<String, String> fieldErrors
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException exception) {
            fieldErrors.put(field, invalidMessage);
            return null;
        }
    }

    private static String parseMedicationName(String medicationName, Map<String, String> fieldErrors) {
        if (medicationName == null || medicationName.isBlank()) {
            fieldErrors.put("medicationName", "Medication name is required.");
            return null;
        }

        String normalized = medicationName.trim();
        if (normalized.length() > 200) {
            fieldErrors.put("medicationName", "Medication name must have at most 200 characters.");
            return null;
        }
        return normalized;
    }

    private static String parseActiveIngredient(String activeIngredient, Map<String, String> fieldErrors) {
        if (activeIngredient == null || activeIngredient.isBlank()) {
            return null;
        }

        String normalized = activeIngredient.trim();
        if (normalized.length() > 200) {
            fieldErrors.put("activeIngredient", "Active ingredient must have at most 200 characters.");
            return null;
        }
        return normalized;
    }

    private static String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String parseDosage(String dosage, Map<String, String> fieldErrors) {
        if (dosage == null || dosage.isBlank()) {
            fieldErrors.put("dosage", "Dosage is required.");
            return null;
        }

        String normalized = dosage.trim();
        if (normalized.length() > 100) {
            fieldErrors.put("dosage", "Dosage must have at most 100 characters.");
            return null;
        }
        return normalized;
    }

    private static Integer parseWithdrawalPeriodDays(String withdrawalPeriodDays, Map<String, String> fieldErrors) {
        if (withdrawalPeriodDays == null || withdrawalPeriodDays.isBlank()) {
            fieldErrors.put("withdrawalPeriodDays", "Withdrawal period is required.");
            return null;
        }

        try {
            int parsed = Integer.parseInt(withdrawalPeriodDays.trim());
            if (parsed < 0) {
                fieldErrors.put("withdrawalPeriodDays", "Withdrawal period must be zero or greater.");
                return null;
            }
            return parsed;
        } catch (NumberFormatException exception) {
            fieldErrors.put("withdrawalPeriodDays", "Withdrawal period must be a whole number.");
            return null;
        }
    }

    private static LocalDate parseApplicationDate(String applicationDate, Map<String, String> fieldErrors) {
        if (applicationDate == null || applicationDate.isBlank()) {
            fieldErrors.put("applicationDate", "Application date is required.");
            return null;
        }

        try {
            return LocalDate.parse(applicationDate.trim());
        } catch (DateTimeParseException exception) {
            fieldErrors.put("applicationDate", "Application date must be a valid ISO date.");
            return null;
        }
    }

    private static BigDecimal parseCost(String cost, Map<String, String> fieldErrors) {
        if (cost == null || cost.isBlank()) {
            return null;
        }

        try {
            BigDecimal parsed = new BigDecimal(cost.trim());
            BigDecimal normalized = parsed.stripTrailingZeros();
            int scale = Math.max(normalized.scale(), 0);
            if (scale > 2) {
                fieldErrors.put("cost", "Cost must have at most 2 decimal places.");
                return null;
            }
            if (parsed.signum() < 0) {
                fieldErrors.put("cost", "Cost must be zero or greater.");
                return null;
            }
            if (parsed.compareTo(new BigDecimal("99999999.99")) > 0) {
                fieldErrors.put("cost", "Cost exceeds the supported maximum.");
                return null;
            }
            return parsed.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException | NumberFormatException exception) {
            fieldErrors.put("cost", "Cost must be a valid number.");
            return null;
        }
    }
}
