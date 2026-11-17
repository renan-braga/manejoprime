package br.com.curralzap.backend.users.application;

import br.com.curralzap.backend.users.domain.UserRole;

import java.util.LinkedHashMap;
import java.util.Map;

public record UserListQuery(
        int limit,
        int offset,
        String search,
        String roleDatabaseValue,
        Boolean active
) {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    public static UserListQuery fromApi(
            String limit,
            String offset,
            String search,
            String role,
            String active
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        Integer parsedLimit = parseLimit(limit, fieldErrors);
        Integer parsedOffset = parseOffset(offset, fieldErrors);
        String parsedRole = parseRole(role, fieldErrors);
        Boolean parsedActive = parseActive(active, fieldErrors);

        if (!fieldErrors.isEmpty()) {
            throw new InvalidUserQueryException(fieldErrors);
        }

        return new UserListQuery(
                parsedLimit,
                parsedOffset,
                normalizeText(search),
                parsedRole,
                parsedActive
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

    private static String parseRole(String role, Map<String, String> fieldErrors) {
        if (role == null || role.isBlank()) {
            return null;
        }

        try {
            return UserRole.fromApiValue(role.trim().toLowerCase()).databaseValue();
        } catch (IllegalArgumentException exception) {
            fieldErrors.put("role", "Role must be admin, veterinarian, or worker.");
            return null;
        }
    }

    private static Boolean parseActive(String active, Map<String, String> fieldErrors) {
        if (active == null || active.isBlank()) {
            return null;
        }

        String normalized = active.trim().toLowerCase();
        if ("true".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized)) {
            return false;
        }

        fieldErrors.put("active", "Active must be true or false.");
        return null;
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
