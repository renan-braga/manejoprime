package br.com.curralzap.backend.auth.domain;

import java.util.regex.Pattern;

public record PhoneNumber(String normalized) {

    private static final Pattern ALLOWED_CHARACTERS = Pattern.compile("^[+()\\-\\s\\d]+$");

    public static PhoneNumber parse(String rawPhone) {
        if (rawPhone == null) {
            throw new InvalidPhoneFormatException();
        }

        String trimmedPhone = rawPhone.trim();
        if (trimmedPhone.isEmpty() || !ALLOWED_CHARACTERS.matcher(trimmedPhone).matches()) {
            throw new InvalidPhoneFormatException();
        }

        String normalizedPhone = trimmedPhone.replaceAll("\\D", "");
        if (normalizedPhone.length() < 10 || normalizedPhone.length() > 15) {
            throw new InvalidPhoneFormatException();
        }

        return new PhoneNumber(normalizedPhone);
    }
}
