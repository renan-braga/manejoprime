package br.com.curralzap.backend.whatsapp.application;

import br.com.curralzap.backend.whatsapp.domain.WhatsappExtractedPayload;
import br.com.curralzap.backend.whatsapp.domain.WhatsappFarmSession;
import br.com.curralzap.backend.whatsapp.domain.WhatsappFarmSessionRepository;
import br.com.curralzap.backend.whatsapp.domain.WhatsappInboundMessage;
import br.com.curralzap.backend.whatsapp.domain.WhatsappOutboundMessage;
import br.com.curralzap.backend.whatsapp.domain.WhatsappOutboundStatus;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStatus;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStep;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class WhatsappOutboundDeliveryService {

    private static final TypeReference<Map<String, Object>> PAYLOAD_MAP = new TypeReference<>() {
    };

    private final WhatsappInboundMessageRepository inboundMessageRepository;
    private final WhatsappExtractedPayloadRepository extractedPayloadRepository;
    private final WhatsappOutboundMessageRepository outboundMessageRepository;
    private final WhatsappOutboundGateway outboundGateway;
    private final WhatsappFarmSessionRepository farmSessionRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public WhatsappOutboundDeliveryService(
            WhatsappInboundMessageRepository inboundMessageRepository,
            WhatsappExtractedPayloadRepository extractedPayloadRepository,
            WhatsappOutboundMessageRepository outboundMessageRepository,
            WhatsappOutboundGateway outboundGateway,
            WhatsappFarmSessionRepository farmSessionRepository,
            ObjectMapper objectMapper
    ) {
        this(
                inboundMessageRepository,
                extractedPayloadRepository,
                outboundMessageRepository,
                outboundGateway,
                farmSessionRepository,
                objectMapper,
                Clock.systemUTC()
        );
    }

    WhatsappOutboundDeliveryService(
            WhatsappInboundMessageRepository inboundMessageRepository,
            WhatsappExtractedPayloadRepository extractedPayloadRepository,
            WhatsappOutboundMessageRepository outboundMessageRepository,
            WhatsappOutboundGateway outboundGateway,
            WhatsappFarmSessionRepository farmSessionRepository,
            Clock clock
    ) {
        this(
                inboundMessageRepository,
                extractedPayloadRepository,
                outboundMessageRepository,
                outboundGateway,
                farmSessionRepository,
                new ObjectMapper(),
                clock
        );
    }

    WhatsappOutboundDeliveryService(
            WhatsappInboundMessageRepository inboundMessageRepository,
            WhatsappExtractedPayloadRepository extractedPayloadRepository,
            WhatsappOutboundMessageRepository outboundMessageRepository,
            WhatsappOutboundGateway outboundGateway,
            WhatsappFarmSessionRepository farmSessionRepository,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.inboundMessageRepository = inboundMessageRepository;
        this.extractedPayloadRepository = extractedPayloadRepository;
        this.outboundMessageRepository = outboundMessageRepository;
        this.outboundGateway = outboundGateway;
        this.farmSessionRepository = farmSessionRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public void handleRequestedDelivery(WhatsappOutboundDeliveryRequestedEvent event) {
        WhatsappInboundMessage inboundMessage = inboundMessageRepository.findById(event.inboundMessageId())
                .orElseThrow(() -> new IllegalStateException(
                        "Inbound message not found for outbound delivery: " + event.inboundMessageId()
                ));
        // Use event.status() to avoid a race with the validation transaction commit
        if (event.status() != WhatsappProcessingStatus.AWAITING_CONFIRMATION
                && event.status() != WhatsappProcessingStatus.REJECTED) {
            return;
        }
        if (hasAlreadyQueuedOrSentOutbound(inboundMessage)) {
            return;
        }

        if (event.status() == WhatsappProcessingStatus.AWAITING_CONFIRMATION) {
            sendButtonsConfirmation(inboundMessage, event);
        } else {
            sendTextRejection(inboundMessage, event);
        }
    }

    private boolean hasAlreadyQueuedOrSentOutbound(WhatsappInboundMessage inboundMessage) {
        return outboundMessageRepository.findByInboundMessageIdOrderByCreatedAtAsc(inboundMessage.id()).stream()
                .anyMatch(message -> message.status() == WhatsappOutboundStatus.QUEUED
                        || message.status() == WhatsappOutboundStatus.SENT);
    }

    private void sendButtonsConfirmation(WhatsappInboundMessage inboundMessage, WhatsappOutboundDeliveryRequestedEvent event) {
        WhatsappExtractedPayload extractedPayload = extractedPayloadRepository
                .findByInboundMessageId(inboundMessage.id())
                .orElseThrow(() -> new IllegalStateException(
                        "Extracted payload not found for outbound delivery: " + inboundMessage.id()
                ));
        Map<String, Object> payload = parsePayload(extractedPayload.structuredPayload());
        WhatsappButtonsOutboundMessage buttonMessage = buildButtonsMessage(
                extractedPayload.intent(), payload, inboundMessage
        );
        WhatsappOutboundMessage queued = outboundMessageRepository.save(WhatsappOutboundMessage.queue(
                inboundMessage.id(),
                inboundMessage.farmId(),
                inboundMessage.userId(),
                inboundMessage.correlationId(),
                inboundMessage.phoneNumber(),
                "buttons",
                outboundButtonsPayload(buttonMessage),
                OffsetDateTime.now(clock)
        ));
        try {
            WhatsappOutboundDeliveryResult result = outboundGateway.sendButtons(buttonMessage);
            outboundMessageRepository.save(
                    queued.markSent(result == null ? null : result.providerMessageId(), OffsetDateTime.now(clock))
            );
        } catch (RuntimeException exception) {
            outboundMessageRepository.save(
                    queued.markFailed(deliveryFailureReason(exception), OffsetDateTime.now(clock))
            );
            throw new WhatsappWorkerRetryableException(
                    inboundMessage.id(),
                    inboundMessage.providerMessageId(),
                    "Failed to process WhatsApp outbound delivery worker event.",
                    exception
            );
        }
    }

    private void sendTextRejection(WhatsappInboundMessage inboundMessage, WhatsappOutboundDeliveryRequestedEvent event) {
        String details = latestProcessingDetails(inboundMessage.id(), WhatsappProcessingStatus.REJECTED)
                .orElseThrow(() -> new WhatsappWorkerRetryableException(
                        event.inboundMessageId(),
                        event.providerMessageId(),
                        "Rejection details not yet visible — retrying after validation transaction commit",
                        new IllegalStateException("Rejection processing step not found")
                ));
        String text = rejectionText(details);
        WhatsappOutboundMessage queued = outboundMessageRepository.save(WhatsappOutboundMessage.queue(
                inboundMessage.id(),
                inboundMessage.farmId(),
                inboundMessage.userId(),
                inboundMessage.correlationId(),
                inboundMessage.phoneNumber(),
                "text",
                outboundPayload(text, inboundMessage.providerMessageId()),
                OffsetDateTime.now(clock)
        ));
        try {
            WhatsappOutboundDeliveryResult result = outboundGateway.sendText(new WhatsappTextOutboundMessage(
                    inboundMessage.phoneNumber(),
                    text,
                    inboundMessage.providerMessageId()
            ));
            outboundMessageRepository.save(
                    queued.markSent(result == null ? null : result.providerMessageId(), OffsetDateTime.now(clock))
            );
        } catch (RuntimeException exception) {
            outboundMessageRepository.save(
                    queued.markFailed(deliveryFailureReason(exception), OffsetDateTime.now(clock))
            );
            throw new WhatsappWorkerRetryableException(
                    inboundMessage.id(),
                    inboundMessage.providerMessageId(),
                    "Failed to process WhatsApp outbound delivery worker event.",
                    exception
            );
        }
    }

    private WhatsappButtonsOutboundMessage buildButtonsMessage(
            String intent,
            Map<String, Object> payload,
            WhatsappInboundMessage inboundMessage
    ) {
        String title = confirmationTitle(intent, payload);
        String farmPrefix = farmNamePrefix(inboundMessage);
        if (!farmPrefix.isBlank()) {
            title = farmPrefix + title;
        }
        String description = intentDescription(intent);
        String confirmId = "confirm:" + inboundMessage.id();
        String cancelId = "cancel:" + inboundMessage.id();
        return new WhatsappButtonsOutboundMessage(
                inboundMessage.phoneNumber(),
                title,
                description,
                "ManejoPrime",
                List.of(
                        new WhatsappButtonsOutboundMessage.Button(confirmId, "✅ Confirmar"),
                        new WhatsappButtonsOutboundMessage.Button(cancelId, "❌ Cancelar")
                ),
                inboundMessage.providerMessageId()
        );
    }

    private String farmNamePrefix(WhatsappInboundMessage inboundMessage) {
        return farmSessionRepository
                .findByPhoneAndDate(inboundMessage.phoneNumber(), LocalDate.now(clock))
                .map(session -> "🏡 *" + session.farmName() + "*\n\n")
                .orElse("");
    }

    private String confirmationTitle(String intent, Map<String, Object> payload) {
        return switch (intent) {
            case "register_weighing" -> "Registrar *" + numberValue(payload, "weightKg") + " kg* para o animal *" + subject(payload) + "*?";
            case "register_occurrence" -> "Registrar ocorrência em *" + subject(payload) + "*: " + stringValue(payload, "description") + "?";
            case "register_feeding" -> {
                String qty = numberValue(payload, "quantityKg");
                if (qty.isBlank()) qty = numberValue(payload, "amountKg");
                yield qty.isBlank()
                        ? "Registrar alimentação para *" + subject(payload) + "*?"
                        : "Registrar *" + qty + " kg* de ração para *" + subject(payload) + "*?";
            }
            case "register_vaccine" -> "Registrar vacina *" + firstPresent(payload, "vaccineName", "name") + "* em *" + subject(payload) + "*?";
            case "register_medication" -> "Registrar medicação *" + firstPresent(payload, "medicationName", "name") + "* em *" + subject(payload) + "*?";
            default -> "Confirmar registro?";
        };
    }

    private String intentDescription(String intent) {
        return switch (intent) {
            case "register_weighing" -> "⚖️ Pesagem";
            case "register_occurrence" -> "🩺 Ocorrência";
            case "register_feeding" -> "🌾 Alimentação";
            case "register_vaccine" -> "💉 Vacinação";
            case "register_medication" -> "💊 Medicação";
            default -> "Registro";
        };
    }

    private String confirmationText(String intent, Map<String, Object> payload) {
        return switch (intent) {
            case "register_weighing" -> "⚖️ " + subject(payload) + ": " + numberValue(payload, "weightKg") + " kg — confirma?";
            case "register_occurrence" -> occurrenceConfirmation(payload);
            case "register_feeding" -> feedingConfirmation(payload);
            case "register_vaccine" -> vaccineConfirmation(payload);
            case "register_medication" -> medicationConfirmation(payload);
            default -> "✅ Revisei sua mensagem — confirma?";
        };
    }

    private String occurrenceConfirmation(Map<String, Object> payload) {
        String severity = stringValue(payload, "severity");
        String details = "🩺 " + subject(payload) + ": " + stringValue(payload, "description");
        if (!severity.isBlank()) {
            details += ", gravidade " + severity;
        }
        return details + " — confirma?";
    }

    private String feedingConfirmation(Map<String, Object> payload) {
        String quantity = numberValue(payload, "quantityKg");
        if (quantity.isBlank()) {
            quantity = numberValue(payload, "amountKg");
        }
        String subject = subject(payload);
        if (quantity.isBlank()) {
            return "🌾 " + subject + ": alimentação — confirma?";
        }
        return "🌾 " + subject + ": alimentação, " + quantity + " kg — confirma?";
    }

    private String vaccineConfirmation(Map<String, Object> payload) {
        String vaccineName = firstPresent(payload, "vaccineName", "name");
        String dose = stringValue(payload, "dose");
        String batch = firstPresent(payload, "batch", "vaccineBatch", "lotNumber");
        StringBuilder builder = new StringBuilder("💉 ").append(subject(payload)).append(": ").append(vaccineName);
        if (!dose.isBlank()) {
            builder.append(", ").append(dose);
        }
        if (!batch.isBlank()) {
            builder.append(", lote ").append(batch);
        }
        return builder.append(" — confirma?").toString();
    }

    private String medicationConfirmation(Map<String, Object> payload) {
        String medicationName = firstPresent(payload, "medicationName", "name");
        String dosage = firstPresent(payload, "dosage", "dose");
        String withdrawalDays = numberValue(payload, "withdrawalDays");
        StringBuilder builder = new StringBuilder("💊 ").append(subject(payload)).append(": ").append(medicationName);
        if (!dosage.isBlank()) {
            builder.append(", ").append(dosage);
        }
        if (!withdrawalDays.isBlank()) {
            builder.append(", carência ").append(withdrawalDays).append(" dias");
        }
        return builder.append(" — confirma?").toString();
    }

    private static final String HELP_MENU_TEXT =
            "🎙️ *Comandos disponíveis por áudio:*\n\n" +
            "⚖️ *Pesagem*\n\"Boi 45 pesou 380 kg\"\n\n" +
            "🩺 *Ocorrência*\n\"Animal 12 está mancando\"\n\n" +
            "🌾 *Alimentação*\n\"Alimentação do lote A, 200 kg\"\n\n" +
            "💉 *Vacinação*\n\"Vacinei o animal 7 com aftosa\"\n\n" +
            "💊 *Medicação*\n\"Apliquei Baymec no animal 3\"\n\n" +
            "Envie um áudio e confirme com os botões! 🤙";

    private String rejectionText(String details) {
        if (details.contains("unauthorized phone number")) {
            return "🔒 Este número não está autorizado no ManejoPrime.\nPeça para um admin cadastrar seu telefone.";
        }
        if (details.contains("permission denied")) {
            return "🚫 Você não tem permissão para registrar " + pluralIntentName(details) + ".\nPeça ao veterinário ou admin.";
        }
        if (details.contains("help_menu")) {
            return HELP_MENU_TEXT;
        }
        if (details.contains("unsupported intent")) {
            return "❓ Não entendi o comando.\nEnvie um áudio dizendo *\"ajuda\"* para ver o que posso fazer. 🎙️";
        }
        if (details.contains("plan limit")) {
            return "📊 Você atingiu o limite diário do plano Free para pesagens.\nPara continuar, use o app ou faça upgrade. 🚀";
        }
        if (details.contains("missing animal")) {
            return "🐄 Não encontrei o animal informado.\nConfira o identificador e envie novamente.";
        }
        if (details.contains("missing lot")) {
            return "📋 Não encontrei o lote informado.\nConfira o identificador e envie novamente.";
        }
        return "⚠️ Não consegui validar essa mensagem.\nConfira os dados e envie novamente.";
    }

    private Optional<String> latestProcessingDetails(UUID inboundMessageId, WhatsappProcessingStatus status) {
        return inboundMessageRepository.findProcessingSteps(inboundMessageId).stream()
                .filter(step -> step.status() == status)
                .max(Comparator.comparing(WhatsappProcessingStep::occurredAt))
                .map(WhatsappProcessingStep::details);
    }

    private String pluralIntentName(String details) {
        if (details.contains("register_vaccine")) {
            return "vacinas";
        }
        if (details.contains("register_medication")) {
            return "medicamentos";
        }
        if (details.contains("register_feeding")) {
            return "alimentações";
        }
        if (details.contains("register_occurrence")) {
            return "ocorrências";
        }
        if (details.contains("register_weighing")) {
            return "pesagens";
        }
        return "essa operação";
    }

    private String subject(Map<String, Object> payload) {
        String animal = firstPresent(payload, "animalIdentifier", "animalName", "animalId");
        if (!animal.isBlank()) {
            return animal;
        }
        String lot = firstPresent(payload, "lotName", "lotIdentifier", "lotId");
        if (!lot.isBlank()) {
            return "Lote " + lot;
        }
        return "Mensagem";
    }

    private String firstPresent(Map<String, Object> payload, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String value = stringValue(payload, fieldName);
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String stringValue(Map<String, Object> payload, String fieldName) {
        Object value = payload.get(fieldName);
        if (value == null) {
            return "";
        }
        return value.toString().trim();
    }

    private String numberValue(Map<String, Object> payload, String fieldName) {
        Object value = payload.get(fieldName);
        if (value == null) {
            return "";
        }
        try {
            return new BigDecimal(value.toString()).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException exception) {
            return value.toString().trim();
        }
    }

    private Map<String, Object> parsePayload(String structuredPayload) {
        try {
            return objectMapper.readValue(structuredPayload, PAYLOAD_MAP);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Extracted payload is not valid JSON.", exception);
        }
    }

    private String outboundPayload(String text, String quotedProviderMessageId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("text", text);
        payload.put("quotedProviderMessageId", quotedProviderMessageId);
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize outbound WhatsApp payload.", exception);
        }
    }

    private String outboundButtonsPayload(WhatsappButtonsOutboundMessage message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", message.title());
        payload.put("description", message.description());
        payload.put("quotedProviderMessageId", message.quotedProviderMessageId());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize outbound WhatsApp buttons payload.", exception);
        }
    }

    private String deliveryFailureReason(RuntimeException exception) {
        if (exception.getMessage() == null || exception.getMessage().isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return exception.getMessage();
    }
}
