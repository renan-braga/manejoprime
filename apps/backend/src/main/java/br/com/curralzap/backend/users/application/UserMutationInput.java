package br.com.curralzap.backend.users.application;

import br.com.curralzap.backend.auth.domain.InvalidPhoneFormatException;
import br.com.curralzap.backend.auth.domain.PhoneNumber;
import br.com.curralzap.backend.users.domain.UserRole;

import java.util.LinkedHashMap;
import java.util.Map;

public record UserMutationInput(
        String phone,
        String name,
        UserRole role
) {

    public static UserMutationInput fromApi(String phone, String name, String role) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        String parsedName = parseName(name, fieldErrors);
        String parsedPhone = parsePhone(phone, fieldErrors);
        UserRole parsedRole = parseRole(role, fieldErrors);

        if (!fieldErrors.isEmpty()) {
            throw new InvalidUserMutationException(fieldErrors);
        }

        return new UserMutationInput(parsedPhone, parsedName, parsedRole);
    }

    private static String parseName(String name, Map<String, String> fieldErrors) {
        if (name == null || name.isBlank()) {
            fieldErrors.put("name", "Name is required.");
            return null;
        }

        String normalized = name.trim();
        if (normalized.length() > 255) {
            fieldErrors.put("name", "Name must be 255 characters or fewer.");
            return null;
        }

        return normalized;
    }

    private static String parsePhone(String phone, Map<String, String> fieldErrors) {
        try {
            return PhoneNumber.parse(phone).normalized();
        } catch (InvalidPhoneFormatException exception) {
            fieldErrors.put("phone", exception.getMessage());
            return null;
        }
    }

    private static UserRole parseRole(String role, Map<String, String> fieldErrors) {
        if (role == null || role.isBlank()) {
            fieldErrors.put("role", "Role is required.");
            return null;
        }

        try {
            return UserRole.fromApiValue(role.trim().toLowerCase());
        } catch (IllegalArgumentException exception) {
            fieldErrors.put("role", "Role must be admin, veterinarian, or worker.");
            return null;
        }
    }
}
