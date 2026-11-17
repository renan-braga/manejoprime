package br.com.curralzap.backend.whatsapp.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface WhatsappAudioMediaJpaSpringRepository extends JpaRepository<WhatsappAudioMediaJpaEntity, UUID> {

    Optional<WhatsappAudioMediaJpaEntity> findByInboundMessageId(UUID inboundMessageId);
}
