package br.com.curralzap.backend.whatsapp.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface WhatsappInboundMessageJpaSpringRepository extends JpaRepository<WhatsappInboundMessageJpaEntity, UUID> {

    Optional<WhatsappInboundMessageJpaEntity> findByProviderMessageId(String providerMessageId);

    List<WhatsappInboundMessageJpaEntity> findByFarmIdOrderByCreatedAtDesc(UUID farmId, Pageable pageable);
}
