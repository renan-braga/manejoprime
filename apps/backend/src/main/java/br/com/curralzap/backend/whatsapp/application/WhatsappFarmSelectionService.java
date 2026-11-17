package br.com.curralzap.backend.whatsapp.application;

import br.com.curralzap.backend.farms.domain.FarmMembershipDetails;
import br.com.curralzap.backend.whatsapp.domain.WhatsappFarmSession;
import br.com.curralzap.backend.whatsapp.domain.WhatsappFarmSessionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class WhatsappFarmSelectionService {

    private final WhatsappFarmSessionRepository sessionRepository;
    private final WhatsappOutboundGateway outboundGateway;
    private final ObjectMapper objectMapper;

    WhatsappFarmSelectionService(
            WhatsappFarmSessionRepository sessionRepository,
            WhatsappOutboundGateway outboundGateway,
            ObjectMapper objectMapper
    ) {
        this.sessionRepository = sessionRepository;
        this.outboundGateway = outboundGateway;
        this.objectMapper = objectMapper;
    }

    public Optional<WhatsappFarmSession> findActiveSession(String phoneNumber, LocalDate date) {
        return sessionRepository.findByPhoneAndDate(phoneNumber, date);
    }

    public Optional<FarmMembershipDetails> parseSelection(String rawPayload, List<FarmMembershipDetails> farms) {
        String text = extractConversationText(rawPayload).trim();
        if (text.isBlank()) {
            return Optional.empty();
        }
        try {
            int index = Integer.parseInt(text) - 1;
            if (index >= 0 && index < farms.size()) {
                return Optional.of(farms.get(index));
            }
        } catch (NumberFormatException ignored) {
        }
        return Optional.empty();
    }

    public void saveSession(String phoneNumber, UUID farmId, String farmName, LocalDate date) {
        sessionRepository.save(phoneNumber, farmId, farmName, date);
    }

    public void sendSelectionPrompt(String phoneNumber, List<FarmMembershipDetails> farms, String quotedMessageId) {
        StringBuilder text = new StringBuilder("🏡 *Qual fazenda você está hoje?*\n\n");
        String[] numbers = {"1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣"};
        for (int i = 0; i < farms.size(); i++) {
            text.append(numbers[Math.min(i, numbers.length - 1)]).append(" ").append(farms.get(i).farmName()).append("\n");
        }
        text.append("\nResponda com o número");
        outboundGateway.sendText(new WhatsappTextOutboundMessage(phoneNumber, text.toString(), quotedMessageId));
    }

    public void sendSelectionConfirmation(String phoneNumber, String farmName, String quotedMessageId) {
        outboundGateway.sendText(new WhatsappTextOutboundMessage(
                phoneNumber,
                "✅ *" + farmName + "* selecionada. Pode mandar!",
                quotedMessageId
        ));
    }

    private String extractConversationText(String rawPayload) {
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            JsonNode message = root.path("data").path("message");
            String conversation = message.path("conversation").asText("");
            if (!conversation.isBlank()) {
                return conversation;
            }
            return message.path("extendedTextMessage").path("text").asText("");
        } catch (Exception ignored) {
            return "";
        }
    }
}
