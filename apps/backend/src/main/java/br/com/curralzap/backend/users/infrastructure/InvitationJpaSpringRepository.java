package br.com.curralzap.backend.users.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface InvitationJpaSpringRepository extends JpaRepository<InvitationJpaEntity, UUID> {

    Optional<InvitationJpaEntity> findFirstByEmailAndStatus(String email, String status);

    List<InvitationJpaEntity> findByFarmId(UUID farmId);

    Optional<InvitationJpaEntity> findByIdAndFarmId(UUID id, UUID farmId);
}
