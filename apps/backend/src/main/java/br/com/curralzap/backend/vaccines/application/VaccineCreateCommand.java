package br.com.curralzap.backend.vaccines.application;

import br.com.curralzap.backend.vaccines.domain.VaccineDose;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record VaccineCreateCommand(
        UUID animalId,
        UUID lotId,
        String vaccineType,
        String vaccineBatch,
        VaccineDose dose,
        LocalDate applicationDate,
        LocalDate nextDoseDate,
        BigDecimal volumeMl
) {
    public static VaccineCreateCommand fromApi(
            String animalId,
            String lotId,
            String vaccineType,
            String vaccineBatch,
            String dose,
            String applicationDate,
            String nextDoseDate,
            String volumeMl
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        UUID parsedAnimalId = parseOptionalUuid(animalId);
        UUID parsedLotId = parseOptionalUuid(lotId);
        validateTarget(animalId, lotId, parsedAnimalId, parsedLotId, fieldErrors);
        String parsedVaccineType = parseVaccineType(vaccineType, fieldErrors);
        String parsedVaccineBatch = parseVaccineBatch(vaccineBatch, fieldErrors);
        VaccineDose parsedDose = parseDose(dose, fieldErrors);
        LocalDate parsedApplicationDate = parseApplicationDate(applicationDate, fieldErrors);
        LocalDate parsedNextDoseDate = parseNextDoseDate(nextDoseDate, fieldErrors);
        BigDecimal parsedVolumeMl = parseVolumeMl(volumeMl, fieldErrors);

        if (parsedApplicationDate != null && parsedNextDoseDate != null && parsedNextDoseDate.isBefore(parsedApplicationDate)) {
            fieldErrors.put("nextDoseDate", "Next dose date must be on or after application date.");
        }

        if (!fieldErrors.isEmpty()) {
            throw new InvalidVaccineMutationException(fieldErrors);
        }

        return new VaccineCreateCommand(
                parsedAnimalId,
                parsedLotId,
                parsedVaccineType,
                parsedVaccineBatch,
                parsedDose,
                parsedApplicationDate,
                parsedNextDoseDate,
                parsedVolumeMl
        );
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

    private static String parseVaccineType(String vaccineType, Map<String, String> fieldErrors) {
        if (vaccineType == null || vaccineType.isBlank()) {
            fieldErrors.put("vaccineType", "Vaccine type is required.");
            return null;
        }

        String normalized = vaccineType.trim();
        if (normalized.length() > 200) {
            fieldErrors.put("vaccineType", "Vaccine type must have at most 200 characters.");
            return null;
        }
        return normalized;
    }

    private static String parseVaccineBatch(String vaccineBatch, Map<String, String> fieldErrors) {
        if (vaccineBatch == null || vaccineBatch.isBlank()) {
            return null;
        }

        String normalized = vaccineBatch.trim();
        if (normalized.length() > 100) {
            fieldErrors.put("vaccineBatch", "Vaccine batch must have at most 100 characters.");
            return null;
        }
        return normalized;
    }

    private static VaccineDose parseDose(String dose, Map<String, String> fieldErrors) {
        if (dose == null || dose.isBlank()) {
            fieldErrors.put("dose", "Dose is required.");
            return null;
        }

        try {
            return VaccineDose.fromDatabaseValue(dose.trim());
        } catch (IllegalArgumentException exception) {
            fieldErrors.put("dose", "Dose must be primeira, segunda, reforco, or anual.");
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

    private static LocalDate parseNextDoseDate(String nextDoseDate, Map<String, String> fieldErrors) {
        if (nextDoseDate == null || nextDoseDate.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(nextDoseDate.trim());
        } catch (DateTimeParseException exception) {
            fieldErrors.put("nextDoseDate", "Next dose date must be a valid ISO date.");
            return null;
        }
    }

    private static BigDecimal parseVolumeMl(String volumeMl, Map<String, String> fieldErrors) {
        if (volumeMl == null || volumeMl.isBlank()) {
            return null;
        }

        try {
            BigDecimal parsed = new BigDecimal(volumeMl.trim());
            BigDecimal normalized = parsed.stripTrailingZeros();
            int scale = Math.max(normalized.scale(), 0);
            if (scale > 2) {
                fieldErrors.put("volumeMl", "Volume must have at most 2 decimal places.");
                return null;
            }
            if (parsed.signum() <= 0) {
                fieldErrors.put("volumeMl", "Volume must be greater than zero.");
                return null;
            }
            if (parsed.compareTo(new BigDecimal("9999.99")) > 0) {
                fieldErrors.put("volumeMl", "Volume exceeds the supported maximum.");
                return null;
            }
            return parsed.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException | NumberFormatException exception) {
            fieldErrors.put("volumeMl", "Volume must be a valid number.");
            return null;
        }
    }
}
