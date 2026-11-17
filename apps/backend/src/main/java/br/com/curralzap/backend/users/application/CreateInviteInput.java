package br.com.curralzap.backend.users.application;

import br.com.curralzap.backend.auth.domain.InvalidPhoneFormatException;
import br.com.curralzap.backend.auth.domain.PhoneNumber;
import br.com.curralzap.backend.users.domain.UserRole;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public record CreateInviteInput(
        String email,
        String phone,
        UserRole role
) {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$"
    );

    public static CreateInviteInput fromApi(String email, String phone, String role) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        String parsedEmail = parseEmail(email, fieldErrors);
        String parsedPhone = parsePhone(phone, fieldErrors);
        UserRole parsedRole = parseRole(role, fieldErrors);

        if (!fieldErrors.isEmpty()) {
            throw new InvalidInviteInputException(fieldErrors);
        }

        return new CreateInviteInput(parsedEmail, parsedPhone, parsedRole);
    }

    private static String parseEmail(String email, Map<String, String> fieldErrors) {
        if (email == null || email.isBlank()) {
            fieldErrors.put("email", "E-mail é obrigatório.");
            return null;
        }
        String normalized = email.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            fieldErrors.put("email", "E-mail inválido.");
            return null;
        }
        return normalized;
    }

    private static String parsePhone(String phone, Map<String, String> fieldErrors) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        try {
            return PhoneNumber.parse(phone).normalized();
        } catch (InvalidPhoneFormatException exception) {
            fieldErrors.put("phone", exception.getMessage());
            return null;
        }
    }

    private static UserRole parseRole(String role, Map<String, String> fieldErrors) {
        if (role == null || role.isBlank()) {
            fieldErrors.put("role", "Papel é obrigatório.");
            return null;
        }
        try {
            return UserRole.fromApiValue(role.trim().toLowerCase());
        } catch (IllegalArgumentException exception) {
            fieldErrors.put("role", "Papel deve ser admin, veterinarian ou worker.");
            return null;
        }
    }
}
