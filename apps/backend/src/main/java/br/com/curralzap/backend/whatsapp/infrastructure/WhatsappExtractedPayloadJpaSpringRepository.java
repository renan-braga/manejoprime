package br.com.curralzap.backend.whatsapp.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

interface WhatsappExtractedPayloadJpaSpringRepository extends JpaRepository<WhatsappExtractedPayloadJpaEntity, UUID> {

    Optional<WhatsappExtractedPayloadJpaEntity> findByInboundMessageId(UUID inboundMessageId);

    @Query("""
            select count(payload)
            from WhatsappExtractedPayloadJpaEntity payload, WhatsappInboundMessageJpaEntity message
            where payload.inboundMessageId = message.id
              and message.farmId = :farmId
              and payload.intent = :intent
              and message.receivedAt >= :startInclusive
              and message.receivedAt < :endExclusive
            """)
    long countByFarmIdAndIntentAndReceivedAtBetween(
            @Param("farmId") UUID farmId,
            @Param("intent") String intent,
            @Param("startInclusive") OffsetDateTime startInclusive,
            @Param("endExclusive") OffsetDateTime endExclusive
    );
}
