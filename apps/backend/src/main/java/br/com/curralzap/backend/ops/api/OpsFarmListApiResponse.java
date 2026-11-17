package br.com.curralzap.backend.ops.api;

import br.com.curralzap.backend.ops.application.OpsFarmListItemView;

import java.time.OffsetDateTime;
import java.util.UUID;

record OpsFarmListApiResponse(
        UUID farmId,
        String farmName,
        OffsetDateTime createdAt,
        long userCount,
        long animalCount,
        OffsetDateTime lastWhatsappActivity,
        long messagesLast7d,
        long errorsLast7d
) {
    static OpsFarmListApiResponse from(OpsFarmListItemView v) {
        return new OpsFarmListApiResponse(
                v.farmId(), v.farmName(), v.createdAt(),
                v.userCount(), v.animalCount(), v.lastWhatsappActivity(),
                v.messagesLast7d(), v.errorsLast7d());
    }
}
