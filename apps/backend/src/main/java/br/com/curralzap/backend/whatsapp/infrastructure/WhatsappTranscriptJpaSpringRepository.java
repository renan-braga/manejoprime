package br.com.curralzap.backend.whatsapp.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface WhatsappTranscriptJpaSpringRepository extends JpaRepository<WhatsappTranscriptJpaEntity, UUID> {

    Optional<WhatsappTranscriptJpaEntity> findByInboundMessageId(UUID inboundMessageId);
}
