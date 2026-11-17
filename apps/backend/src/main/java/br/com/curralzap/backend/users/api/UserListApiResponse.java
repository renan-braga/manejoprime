package br.com.curralzap.backend.users.api;

import br.com.curralzap.backend.users.application.UserListItemView;
import br.com.curralzap.backend.users.application.UserListView;

import java.util.List;
import java.util.UUID;

record UserListApiResponse(
        List<UserListItemApiResponse> items,
        long total,
        int limit,
        int offset
) {
    static UserListApiResponse from(UserListView users) {
        return new UserListApiResponse(
                users.items().stream().map(UserListItemApiResponse::from).toList(),
                users.total(),
                users.limit(),
                users.offset()
        );
    }

    record UserListItemApiResponse(
            UUID id,
            String name,
            String phone,
            String role,
            boolean active
    ) {
        static UserListItemApiResponse from(UserListItemView user) {
            return new UserListItemApiResponse(
                    user.id(),
                    user.name(),
                    formatPhone(user.phone()),
                    user.role(),
                    user.active()
            );
        }

        private static String formatPhone(String phone) {
            return phone.startsWith("+") ? phone : "+" + phone;
        }
    }
}
