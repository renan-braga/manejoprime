package br.com.curralzap.backend.users.application;

import java.util.Optional;
import java.util.UUID;

public interface UserReadViewRepository {

    UserListView listFor(UUID farmId, UserListQuery query);

    Optional<UserDetailView> findDetailById(UUID farmId, UUID userId);
}
