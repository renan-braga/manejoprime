package br.com.curralzap.backend.whatsapp.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface WhatsappProcessingStepJpaSpringRepository extends JpaRepository<WhatsappProcessingStepJpaEntity, UUID> {

    List<WhatsappProcessingStepJpaEntity> findByInboundMessageIdOrderByOccurredAtAsc(UUID inboundMessageId);
}
