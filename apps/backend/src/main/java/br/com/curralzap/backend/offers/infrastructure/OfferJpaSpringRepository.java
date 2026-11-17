package br.com.curralzap.backend.offers.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface OfferJpaSpringRepository extends JpaRepository<OfferJpaEntity, UUID> {

    Optional<OfferJpaEntity> findByIdAndFarmId(UUID offerId, UUID farmId);
}
