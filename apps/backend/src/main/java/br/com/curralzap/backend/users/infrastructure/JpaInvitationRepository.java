package br.com.curralzap.backend.users.infrastructure;

import br.com.curralzap.backend.users.domain.Invitation;
import br.com.curralzap.backend.users.domain.InvitationRepository;
import br.com.curralzap.backend.users.domain.InvitationStatus;
import br.com.curralzap.backend.users.domain.UserRole;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaInvitationRepository implements InvitationRepository {

    private final InvitationJpaSpringRepository repository;

    JpaInvitationRepository(InvitationJpaSpringRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Invitation> findPendingByEmail(String email) {
        return repository.findFirstByEmailAndStatus(email, InvitationStatus.PENDING.name().toLowerCase())
                .map(this::toDomain);
    }

    @Override
    public List<Invitation> findByFarmId(UUID farmId) {
        return repository.findByFarmId(farmId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Invitation> findByIdAndFarmId(UUID id, UUID farmId) {
        return repository.findByIdAndFarmId(id, farmId).map(this::toDomain);
    }

    @Override
    public Invitation save(Invitation invitation) {
        InvitationJpaEntity entity = invitation.id() == null
                ? new InvitationJpaEntity()
                : repository.findById(invitation.id()).orElseGet(InvitationJpaEntity::new);
        entity.setFarmIdValue(invitation.farmId());
        entity.setEmail(invitation.email());
        entity.setPhone(invitation.phone());
        entity.setRole(invitation.role().databaseValue());
        entity.setStatus(invitation.status().name().toLowerCase());
        entity.setInvitedBy(invitation.invitedBy());
        return toDomain(repository.save(entity));
    }

    private Invitation toDomain(InvitationJpaEntity entity) {
        return new Invitation(
                entity.getId(),
                entity.getFarmId(),
                entity.getEmail(),
                entity.getPhone(),
                UserRole.fromDatabaseValue(entity.getRole()),
                InvitationStatus.valueOf(entity.getStatus().toUpperCase()),
                entity.getInvitedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
