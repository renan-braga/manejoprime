package br.com.curralzap.backend.users.api;

import br.com.curralzap.backend.users.application.UserDetailView;

import java.util.UUID;

record UserApiResponse(
        UUID id,
        UUID farmId,
        String name,
        String phone,
        String role,
        boolean active
) {
    static UserApiResponse from(UserDetailView user) {
        return new UserApiResponse(
                user.id(),
                user.farmId(),
                user.name(),
                formatPhone(user.phone()),
                user.role(),
                user.active()
        );
    }

    private static String formatPhone(String phone) {
        if (phone == null) return null;
        return phone.startsWith("+") ? phone : "+" + phone;
    }
}
