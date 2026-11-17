package br.com.curralzap.backend.shared.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface MediaReferenceJpaSpringRepository extends JpaRepository<MediaReferenceJpaEntity, UUID> {

    Optional<MediaReferenceJpaEntity> findByOwnerTypeAndOwnerIdAndPurpose(
            String ownerType,
            UUID ownerId,
            String purpose
    );

    List<MediaReferenceJpaEntity> findByOwnerTypeAndOwnerIdOrderByPurposeAsc(
            String ownerType,
            UUID ownerId
    );
}
