package br.com.curralzap.backend.users.infrastructure;

import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserAccountRepository;
import br.com.curralzap.backend.users.domain.UserRole;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaUserAccountRepository implements UserAccountRepository {

    private final UserAccountJpaSpringRepository repository;

    JpaUserAccountRepository(UserAccountJpaSpringRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<UserAccount> findByPhone(String normalizedPhone) {
        return repository.findFirstByPhoneIn(List.of(normalizedPhone, "+" + normalizedPhone))
                .map(this::toDomain);
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        return repository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public Optional<UserAccount> findById(UUID userId) {
        return repository.findById(userId).map(this::toDomain);
    }

    @Override
    public Optional<UserAccount> findByIdAndFarmId(UUID userId, UUID farmId) {
        return repository.findByIdAndFarmId(userId, farmId).map(this::toDomain);
    }

    @Override
    public UserAccount save(UserAccount userAccount) {
        UserAccountJpaEntity entity = userAccount.id() == null
                ? new UserAccountJpaEntity()
                : repository.findById(userAccount.id()).orElseGet(UserAccountJpaEntity::new);
        entity.setFarmIdValue(userAccount.farmId());
        entity.setPhone(userAccount.phone());
        entity.setEmail(userAccount.email());
        entity.setName(userAccount.name());
        entity.setRole(userAccount.role().databaseValue());
        entity.setActive(userAccount.active());
        return toDomain(repository.save(entity));
    }

    private UserAccount toDomain(UserAccountJpaEntity entity) {
        return new UserAccount(
                entity.getId(),
                entity.getFarmId(),
                entity.getPhone(),
                entity.getEmail(),
                entity.getName(),
                UserRole.fromDatabaseValue(entity.getRole()),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
