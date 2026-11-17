package br.com.curralzap.backend.offers.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record OfferMutationInput(
        String title,
        String notes,
        BigDecimal askingPrice,
        String buyerContact,
        LocalDate expiresAt,
        List<UUID> animalIds
) {
    public static OfferMutationInput fromApi(
            String title,
            String notes,
            BigDecimal askingPrice,
            String buyerContact,
            LocalDate expiresAt,
            List<UUID> animalIds
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        String parsedTitle = parseRequiredText(title, "title", "Title", 200, fieldErrors);
        BigDecimal parsedPrice = parseAskingPrice(askingPrice, fieldErrors);
        String parsedBuyerContact = parseOptionalText(buyerContact, "buyerContact", "Buyer contact", 200, fieldErrors);

        if (!fieldErrors.isEmpty()) {
            throw new InvalidOfferMutationException(fieldErrors);
        }

        return new OfferMutationInput(
                parsedTitle,
                notes != null && !notes.isBlank() ? notes.trim() : null,
                parsedPrice,
                parsedBuyerContact,
                expiresAt,
                animalIds != null ? animalIds : List.of()
        );
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

    private static BigDecimal parseAskingPrice(BigDecimal value, Map<String, String> fieldErrors) {
        if (value == null) {
            fieldErrors.put("askingPrice", "Asking price is required.");
            return null;
        }

        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            fieldErrors.put("askingPrice", "Asking price must be greater than zero.");
            return null;
        }

        return value;
    }
}
