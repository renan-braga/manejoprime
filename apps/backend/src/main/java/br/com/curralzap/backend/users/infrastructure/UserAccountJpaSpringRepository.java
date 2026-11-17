package br.com.curralzap.backend.users.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

interface UserAccountJpaSpringRepository extends JpaRepository<UserAccountJpaEntity, UUID> {

    Optional<UserAccountJpaEntity> findFirstByPhoneIn(Collection<String> phones);

    Optional<UserAccountJpaEntity> findByEmail(String email);

    Optional<UserAccountJpaEntity> findByIdAndFarmId(UUID userId, UUID farmId);
}
