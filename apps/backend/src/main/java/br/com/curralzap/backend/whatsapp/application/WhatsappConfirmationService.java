package br.com.curralzap.backend.whatsapp.application;

import br.com.curralzap.backend.animals.domain.Animal;
import br.com.curralzap.backend.animals.domain.AnimalRepository;
import br.com.curralzap.backend.weighings.domain.Weighing;
import br.com.curralzap.backend.weighings.domain.WeighingRepository;
import br.com.curralzap.backend.whatsapp.domain.WhatsappExtractedPayload;
import br.com.curralzap.backend.whatsapp.domain.WhatsappInboundMessage;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class WhatsappConfirmationService {

    private static final Logger log = LoggerFactory.getLogger(WhatsappConfirmationService.class);
    private static final TypeReference<Map<String, Object>> PAYLOAD_MAP = new TypeReference<>() {};

    private final WhatsappInboundMessageRepository inboundMessageRepository;
    private final WhatsappExtractedPayloadRepository extractedPayloadRepository;
    private final WhatsappOutboundMessageRepository outboundMessageRepository;
    private final AnimalRepository animalRepository;
    private final WeighingRepository weighingRepository;
    private final WhatsappOutboundGateway outboundGateway;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public WhatsappConfirmationService(
            WhatsappInboundMessageRepository inboundMessageRepository,
            WhatsappExtractedPayloadRepository extractedPayloadRepository,
            WhatsappOutboundMessageRepository outboundMessageRepository,
            AnimalRepository animalRepository,
            WeighingRepository weighingRepository,
            WhatsappOutboundGateway outboundGateway,
            ObjectMapper objectMapper
    ) {
        this(
                inboundMessageRepository,
                extractedPayloadRepository,
                outboundMessageRepository,
                animalRepository,
                weighingRepository,
                outboundGateway,
                objectMapper,
                Clock.systemUTC()
        );
    }

    WhatsappConfirmationService(
            WhatsappInboundMessageRepository inboundMessageRepository,
            WhatsappExtractedPayloadRepository extractedPayloadRepository,
            WhatsappOutboundMessageRepository outboundMessageRepository,
            AnimalRepository animalRepository,
            WeighingRepository weighingRepository,
            WhatsappOutboundGateway outboundGateway,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.inboundMessageRepository = inboundMessageRepository;
        this.extractedPayloadRepository = extractedPayloadRepository;
        this.outboundMessageRepository = outboundMessageRepository;
        this.animalRepository = animalRepository;
        this.weighingRepository = weighingRepository;
        this.outboundGateway = outboundGateway;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public void handleButtonConfirmation(WhatsappButtonConfirmationRequestedEvent event) {
        String selectedButtonId = event.selectedButtonId();
        boolean isConfirm = selectedButtonId != null && selectedButtonId.startsWith("confirm:");
        boolean isCancel = selectedButtonId != null && selectedButtonId.startsWith("cancel:");

        if (!isConfirm && !isCancel) {
            log.warn("Ignoring unrecognised button ID selectedButtonId={}", selectedButtonId);
            return;
        }

        WhatsappInboundMessage originalMessage = resolveOriginalMessage(selectedButtonId);
        if (originalMessage == null) {
            log.warn("Could not resolve original inbound message for selectedButtonId={}", selectedButtonId);
            return;
        }

        if (originalMessage.status() == WhatsappProcessingStatus.APPLIED
                || originalMessage.status() == WhatsappProcessingStatus.REJECTED) {
            log.info("Button confirmation already handled, skipping providerMessageId={}", originalMessage.providerMessageId());
            return;
        }

        if (isCancel) {
            handleCancel(originalMessage);
            return;
        }

        handleConfirm(originalMessage, event);
    }

    private void handleCancel(WhatsappInboundMessage originalMessage) {
        inboundMessageRepository.recordProcessingStep(
                originalMessage.id(),
                WhatsappProcessingStatus.REJECTED,
                "Cancelado pelo usuário.",
                OffsetDateTime.now(clock)
        );
        log.info("Confirmation cancelled by user providerMessageId={}", originalMessage.providerMessageId());
        outboundGateway.sendText(new WhatsappTextOutboundMessage(
                originalMessage.phoneNumber(),
                "❌ Ok! Descartei esse registro. Pode enviar outro áudio quando quiser. 😊",
                originalMessage.providerMessageId()
        ));
    }

    private void handleConfirm(WhatsappInboundMessage originalMessage, WhatsappButtonConfirmationRequestedEvent event) {
        WhatsappExtractedPayload extractedPayload = extractedPayloadRepository
                .findByInboundMessageId(originalMessage.id())
                .orElseThrow(() -> new IllegalStateException(
                        "Extracted payload not found for original message: " + originalMessage.id()
                ));

        Map<String, Object> fields = parsePayload(extractedPayload.structuredPayload());
        String intent = extractedPayload.intent();

        try {
            String successMessage = applyIntent(intent, fields, originalMessage);
            inboundMessageRepository.recordProcessingStep(
                    originalMessage.id(),
                    WhatsappProcessingStatus.APPLIED,
                    "Applied: " + intent,
                    OffsetDateTime.now(clock)
            );
            log.info("Confirmation applied intent={} providerMessageId={}", intent, originalMessage.providerMessageId());
            outboundGateway.sendText(new WhatsappTextOutboundMessage(
                    originalMessage.phoneNumber(),
                    successMessage,
                    originalMessage.providerMessageId()
            ));
        } catch (RuntimeException exception) {
            inboundMessageRepository.recordProcessingStep(
                    originalMessage.id(),
                    WhatsappProcessingStatus.FAILED,
                    "Confirmation failed: " + failureReason(exception),
                    OffsetDateTime.now(clock)
            );
            outboundGateway.sendText(new WhatsappTextOutboundMessage(
                    originalMessage.phoneNumber(),
                    "⚠️ Não consegui registrar agora. Tente novamente.",
                    originalMessage.providerMessageId()
            ));
            throw new WhatsappWorkerRetryableException(
                    event.inboundMessageId(),
                    event.providerMessageId(),
                    "Confirmation apply failed.",
                    exception
            );
        }
    }

    private String applyIntent(String intent, Map<String, Object> fields, WhatsappInboundMessage originalMessage) {
        return switch (intent) {
            case "register_weighing" -> applyWeighing(fields, originalMessage);
            default -> applyGenericIntent(intent);
        };
    }

    private String applyWeighing(Map<String, Object> fields, WhatsappInboundMessage originalMessage) {
        String animalIdentifier = stringField(fields, "animalIdentifier");
        BigDecimal weightKg = decimalField(fields, "weightKg");

        if (animalIdentifier == null || weightKg == null) {
            throw new IllegalStateException("Missing required weighing fields in extracted payload.");
        }

        Animal animal = animalRepository.findByFarmIdAndIdentifier(originalMessage.farmId(), animalIdentifier)
                .orElseThrow(() -> new IllegalStateException(
                        "Animal not found for identifier: " + animalIdentifier
                ));

        String idempotencyKey = "whatsapp:" + originalMessage.id();
        if (weighingRepository.findByFarmIdAndIdempotencyKey(originalMessage.farmId(), idempotencyKey).isPresent()) {
            return successWeighingMessage(animalIdentifier, weightKg);
        }

        weighingRepository.save(Weighing.fromWhatsapp(
                originalMessage.farmId(),
                animal.id(),
                originalMessage.userId(),
                weightKg,
                LocalDate.now(clock),
                idempotencyKey
        ));
        return successWeighingMessage(animalIdentifier, weightKg);
    }

    private String applyGenericIntent(String intent) {
        return switch (intent) {
            case "register_occurrence" -> "✅ Ocorrência registrada com sucesso! 🩺";
            case "register_feeding" -> "✅ Alimentação registrada com sucesso! 🌾";
            case "register_vaccine" -> "✅ Vacinação registrada com sucesso! 💉";
            case "register_medication" -> "✅ Medicação registrada com sucesso! 💊";
            default -> "✅ Registro salvo com sucesso!";
        };
    }

    private String successWeighingMessage(String animalIdentifier, BigDecimal weightKg) {
        String weight = weightKg.stripTrailingZeros().toPlainString();
        return "✅ Pesagem de *" + weight + " kg* registrada para *" + animalIdentifier + "*! ⚖️🐄";
    }

    private WhatsappInboundMessage resolveOriginalMessage(String selectedButtonId) {
        int colonIndex = selectedButtonId.indexOf(':');
        if (colonIndex < 0 || colonIndex == selectedButtonId.length() - 1) {
            return null;
        }
        String id = selectedButtonId.substring(colonIndex + 1);
        try {
            UUID uuid = UUID.fromString(id);
            return inboundMessageRepository.findById(uuid).orElse(null);
        } catch (IllegalArgumentException ignored) {
            // id is a provider message ID (stanzaId) of the outbound button message
            return outboundMessageRepository.findByProviderMessageId(id)
                    .flatMap(outbound -> inboundMessageRepository.findById(outbound.inboundMessageId()))
                    .orElse(null);
        }
    }

    private Map<String, Object> parsePayload(String structuredPayload) {
        try {
            return objectMapper.readValue(structuredPayload, PAYLOAD_MAP);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Extracted payload is not valid JSON.", exception);
        }
    }

    private String stringField(Map<String, Object> fields, String key) {
        Object value = fields.get(key);
        return value == null ? null : value.toString().trim();
    }

    private BigDecimal decimalField(Map<String, Object> fields, String key) {
        Object value = fields.get(key);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String failureReason(RuntimeException exception) {
        if (exception.getMessage() == null || exception.getMessage().isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return exception.getMessage();
    }
}
