package br.com.curralzap.backend.whatsapp.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface WhatsappOutboundMessageJpaSpringRepository extends JpaRepository<WhatsappOutboundMessageJpaEntity, UUID> {

    Optional<WhatsappOutboundMessageJpaEntity> findByProviderMessageId(String providerMessageId);

    List<WhatsappOutboundMessageJpaEntity> findByInboundMessageIdOrderByCreatedAtAsc(UUID inboundMessageId);
}
