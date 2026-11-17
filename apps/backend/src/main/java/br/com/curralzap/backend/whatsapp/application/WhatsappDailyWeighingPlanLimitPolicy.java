package br.com.curralzap.backend.whatsapp.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
class WhatsappDailyWeighingPlanLimitPolicy implements WhatsappPlanLimitPolicy {

    private static final String WEIGHING_INTENT = "register_weighing";

    private final WhatsappExtractedPayloadRepository extractedPayloadRepository;
    private final int freeTierDailyWeighingLimit;

    WhatsappDailyWeighingPlanLimitPolicy(
            WhatsappExtractedPayloadRepository extractedPayloadRepository,
            @Value("${curralzap.whatsapp.validation.free-tier-daily-weighing-limit:10}") int freeTierDailyWeighingLimit
    ) {
        this.extractedPayloadRepository = extractedPayloadRepository;
        this.freeTierDailyWeighingLimit = freeTierDailyWeighingLimit;
    }

    @Override
    public WhatsappPlanLimitResult check(WhatsappPlanLimitRequest request) {
        if (!WEIGHING_INTENT.equals(request.intent()) || freeTierDailyWeighingLimit <= 0) {
            return WhatsappPlanLimitResult.allowed();
        }

        OffsetDateTime dayStart = request.occurredAt()
                .toLocalDate()
                .atStartOfDay()
                .atOffset(request.occurredAt().getOffset());
        long dailyWeighings = extractedPayloadRepository.countByFarmIdAndIntentAndReceivedAtBetween(
                request.farmId(),
                WEIGHING_INTENT,
                dayStart,
                dayStart.plusDays(1)
        );
        if (dailyWeighings > freeTierDailyWeighingLimit) {
            return WhatsappPlanLimitResult.denied("Daily WhatsApp weighing limit reached.");
        }
        return WhatsappPlanLimitResult.allowed();
    }
}
