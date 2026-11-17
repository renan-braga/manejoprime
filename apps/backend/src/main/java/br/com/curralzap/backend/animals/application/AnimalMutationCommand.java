package br.com.curralzap.backend.animals.application;

import br.com.curralzap.backend.animals.domain.AnimalSex;
import br.com.curralzap.backend.animals.domain.AnimalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record AnimalMutationCommand(
        String identifier,
        String name,
        String breed,
        AnimalSex sex,
        LocalDate birthDate,
        BigDecimal birthWeightKg,
        AnimalStatus status,
        LocalDate entryDate,
        UUID lotId
) {
    public static AnimalMutationCommand fromApi(
            String identifier,
            String name,
            String breed,
            String sex,
            String birthDate,
            String birthWeightKg,
            String status,
            String entryDate,
            String lotId
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        String parsedIdentifier = parseIdentifier(identifier, fieldErrors);
        String parsedName = parseOptionalText(name, "name", 100, fieldErrors);
        String parsedBreed = parseOptionalText(breed, "breed", 100, fieldErrors);
        AnimalSex parsedSex = parseSex(sex, fieldErrors);
        LocalDate parsedBirthDate = parseDate(birthDate, "birthDate", fieldErrors);
        BigDecimal parsedBirthWeightKg = parseBirthWeight(birthWeightKg, fieldErrors);
        AnimalStatus parsedStatus = parseStatus(status, fieldErrors);
        LocalDate parsedEntryDate = parseDate(entryDate, "entryDate", fieldErrors);
        UUID parsedLotId = parseOptionalUuid(lotId, "lotId", fieldErrors);

        if (!fieldErrors.isEmpty()) {
            throw new InvalidAnimalMutationException(fieldErrors);
        }

        return new AnimalMutationCommand(
                parsedIdentifier,
                parsedName,
                parsedBreed,
                parsedSex,
                parsedBirthDate,
                parsedBirthWeightKg,
                parsedStatus,
                parsedEntryDate,
                parsedLotId
        );
    }

    private static String parseIdentifier(String identifier, Map<String, String> fieldErrors) {
        if (identifier == null || identifier.isBlank()) {
            fieldErrors.put("identifier", "Identifier is required.");
            return null;
        }

        String normalizedIdentifier = identifier.trim();
        if (normalizedIdentifier.length() > 50) {
            fieldErrors.put("identifier", "Identifier must be 50 characters or fewer.");
            return null;
        }

        return normalizedIdentifier;
    }

    private static String parseOptionalText(String value, String field, int maxLength, Map<String, String> fieldErrors) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalizedValue = value.trim();
        if (normalizedValue.length() > maxLength) {
            fieldErrors.put(field, capitalize(field) + " must be " + maxLength + " characters or fewer.");
            return null;
        }

        return normalizedValue;
    }

    private static AnimalSex parseSex(String sex, Map<String, String> fieldErrors) {
        if (sex == null || sex.isBlank()) {
            fieldErrors.put("sex", "Sex is required.");
            return null;
        }

        try {
            return AnimalSex.fromApiValue(sex.trim());
        } catch (IllegalArgumentException exception) {
            fieldErrors.put("sex", "Sex must be male or female.");
            return null;
        }
    }

    private static AnimalStatus parseStatus(String status, Map<String, String> fieldErrors) {
        if (status == null || status.isBlank()) {
            fieldErrors.put("status", "Status is required.");
            return null;
        }

        try {
            return AnimalStatus.fromApiValue(status.trim());
        } catch (IllegalArgumentException exception) {
            fieldErrors.put("status", "Status must be active, sold, dead, or transferred.");
            return null;
        }
    }

    private static LocalDate parseDate(String value, String field, Map<String, String> fieldErrors) {
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

    private static BigDecimal parseBirthWeight(String birthWeightKg, Map<String, String> fieldErrors) {
        if (birthWeightKg == null || birthWeightKg.isBlank()) {
            return null;
        }

        try {
            BigDecimal parsed = new BigDecimal(birthWeightKg.trim());
            if (parsed.scale() > 2) {
                fieldErrors.put("birthWeightKg", "Birth weight must have at most 2 decimal places.");
                return null;
            }
            if (parsed.signum() < 0) {
                fieldErrors.put("birthWeightKg", "Birth weight must be zero or greater.");
                return null;
            }
            if (parsed.compareTo(new BigDecimal("999999.99")) > 0) {
                fieldErrors.put("birthWeightKg", "Birth weight exceeds the supported maximum.");
                return null;
            }
            return parsed;
        } catch (NumberFormatException exception) {
            fieldErrors.put("birthWeightKg", "Birth weight must be a valid number.");
            return null;
        }
    }

    private static UUID parseOptionalUuid(String value, String field, Map<String, String> fieldErrors) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException exception) {
            fieldErrors.put(field, capitalize(field) + " must be a valid UUID.");
            return null;
        }
    }

    private static String capitalize(String field) {
        return Character.toUpperCase(field.charAt(0)) + field.substring(1);
    }
}
