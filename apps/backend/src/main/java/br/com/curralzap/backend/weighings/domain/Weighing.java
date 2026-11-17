package br.com.curralzap.backend.weighings.domain;

import br.com.curralzap.backend.weighings.application.WeighingCreateCommand;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record Weighing(
        UUID id,
        UUID farmId,
        UUID animalId,
        BigDecimal weightKg,
        LocalDate weighingDate,
        WeighingType type,
        WeighingSource source,
        UUID recordedBy,
        String idempotencyKey,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static Weighing create(
            UUID farmId,
            UUID recordedBy,
            WeighingCreateCommand command,
            String idempotencyKey
    ) {
        return new Weighing(
                UUID.randomUUID(),
                farmId,
                command.animalId(),
                command.weightKg(),
                command.weighingDate(),
                command.type(),
                WeighingSource.APP,
                recordedBy,
                idempotencyKey,
                null,
                null
        );
    }

    public static Weighing fromWhatsapp(
            UUID farmId,
            UUID animalId,
            UUID recordedBy,
            BigDecimal weightKg,
            LocalDate weighingDate,
            String idempotencyKey
    ) {
        return new Weighing(
                UUID.randomUUID(),
                farmId,
                animalId,
                weightKg.setScale(2, RoundingMode.HALF_UP),
                weighingDate,
                WeighingType.ROUTINE,
                WeighingSource.WHATSAPP,
                recordedBy,
                idempotencyKey,
                null,
                null
        );
    }
}
