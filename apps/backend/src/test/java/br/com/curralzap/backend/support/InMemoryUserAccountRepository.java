package br.com.curralzap.backend.support;

import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserAccountRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryUserAccountRepository implements UserAccountRepository {

    private final Map<UUID, UserAccount> accountsById = new HashMap<>();
    private final Map<String, UserAccount> accountsByPhone = new HashMap<>();
    private final Map<String, UserAccount> accountsByEmail = new HashMap<>();

    @Override
    public Optional<UserAccount> findByPhone(String normalizedPhone) {
        return Optional.ofNullable(accountsByPhone.get(normalizedPhone));
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        return Optional.ofNullable(accountsByEmail.get(email));
    }

    @Override
    public Optional<UserAccount> findById(UUID userId) {
        return Optional.ofNullable(accountsById.get(userId));
    }

    @Override
    public Optional<UserAccount> findByIdAndFarmId(UUID userId, UUID farmId) {
        return findById(userId).filter(userAccount -> userAccount.farmId().equals(farmId));
    }

    @Override
    public UserAccount save(UserAccount userAccount) {
        UUID userId = userAccount.id() != null ? userAccount.id() : UUID.randomUUID();
        UserAccount savedUserAccount = userAccount.id() != null
                ? userAccount
                : new UserAccount(
                        userId,
                        userAccount.farmId(),
                        userAccount.phone(),
                        userAccount.email(),
                        userAccount.name(),
                        userAccount.role(),
                        userAccount.active(),
                        userAccount.createdAt(),
                        userAccount.updatedAt()
                );

        accountsByPhone.entrySet().removeIf(entry -> entry.getValue().id().equals(userId));
        accountsByEmail.entrySet().removeIf(entry -> entry.getValue().id().equals(userId));
        accountsById.put(userId, savedUserAccount);
        if (savedUserAccount.phone() != null) {
            accountsByPhone.put(savedUserAccount.phone(), savedUserAccount);
            accountsByPhone.put(savedUserAccount.phone().replaceFirst("^\\+", ""), savedUserAccount);
        }
        if (savedUserAccount.email() != null) {
            accountsByEmail.put(savedUserAccount.email(), savedUserAccount);
        }
        return savedUserAccount;
    }
}
