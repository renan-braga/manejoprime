package br.com.curralzap.backend.users.domain;

import br.com.curralzap.backend.shared.domain.FarmScopedEntityRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends FarmScopedEntityRepository<UserAccount> {

    Optional<UserAccount> findByPhone(String normalizedPhone);

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findById(UUID userId);

    UserAccount save(UserAccount userAccount);
}
