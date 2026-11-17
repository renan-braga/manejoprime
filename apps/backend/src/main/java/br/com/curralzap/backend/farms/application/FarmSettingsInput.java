package br.com.curralzap.backend.farms.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

public record FarmSettingsInput(
        String farmName,
        BigDecimal carcassYieldPercent
) {
    private static final BigDecimal MIN_CARCASS_YIELD_PERCENT = BigDecimal.ZERO;
    private static final BigDecimal MAX_CARCASS_YIELD_PERCENT = new BigDecimal("100.00");

    public static FarmSettingsInput fromApi(String farmName, String carcassYieldPercent) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        String parsedFarmName = parseFarmName(farmName, fieldErrors);
        BigDecimal parsedCarcassYieldPercent = parseCarcassYieldPercent(carcassYieldPercent, fieldErrors);

        if (!fieldErrors.isEmpty()) {
            throw new InvalidFarmSettingsMutationException(fieldErrors);
        }

        return new FarmSettingsInput(parsedFarmName, parsedCarcassYieldPercent);
    }

    private static String parseFarmName(String farmName, Map<String, String> fieldErrors) {
        if (farmName == null || farmName.isBlank()) {
            fieldErrors.put("farmName", "Farm name is required.");
            return null;
        }

        String normalized = farmName.trim();
        if (normalized.length() > 255) {
            fieldErrors.put("farmName", "Farm name must be 255 characters or fewer.");
            return null;
        }

        return normalized;
    }

    private static BigDecimal parseCarcassYieldPercent(
            String carcassYieldPercent,
            Map<String, String> fieldErrors
    ) {
        if (carcassYieldPercent == null || carcassYieldPercent.isBlank()) {
            fieldErrors.put("carcassYieldPercent", "Carcass yield percent is required.");
            return null;
        }

        try {
            BigDecimal parsed = new BigDecimal(carcassYieldPercent.trim());
            BigDecimal normalized = parsed.stripTrailingZeros();
            int scale = Math.max(normalized.scale(), 0);
            if (
                    scale > 2
                            || parsed.compareTo(MIN_CARCASS_YIELD_PERCENT) < 0
                            || parsed.compareTo(MAX_CARCASS_YIELD_PERCENT) > 0
            ) {
                fieldErrors.put(
                        "carcassYieldPercent",
                        "Carcass yield percent must be between 0 and 100 with at most 2 decimal places."
                );
                return null;
            }

            return parsed.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException | NumberFormatException exception) {
            fieldErrors.put(
                    "carcassYieldPercent",
                    "Carcass yield percent must be a number with at most 2 decimal places."
            );
            return null;
        }
    }
}
