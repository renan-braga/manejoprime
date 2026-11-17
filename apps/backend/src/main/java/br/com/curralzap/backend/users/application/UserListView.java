package br.com.curralzap.backend.users.application;

import java.util.List;

public record UserListView(
        List<UserListItemView> items,
        long total,
        int limit,
        int offset
) {
}
