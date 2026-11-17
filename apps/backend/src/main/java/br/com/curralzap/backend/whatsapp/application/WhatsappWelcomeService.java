package br.com.curralzap.backend.whatsapp.application;

import br.com.curralzap.backend.farms.domain.FarmSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class WhatsappWelcomeService {

    private static final Logger log = LoggerFactory.getLogger(WhatsappWelcomeService.class);

    private final WhatsappOutboundGateway outboundGateway;
    private final FarmSettingsRepository farmSettingsRepository;

    WhatsappWelcomeService(WhatsappOutboundGateway outboundGateway, FarmSettingsRepository farmSettingsRepository) {
        this.outboundGateway = outboundGateway;
        this.farmSettingsRepository = farmSettingsRepository;
    }

    public void sendWelcome(String phone, String displayName, UUID farmId) {
        String farmName = farmSettingsRepository.findByFarmId(farmId)
                .map(s -> s.farmName())
                .orElse("ManejoPrime");

        String rawPhone = phone.startsWith("+") ? phone.substring(1) : phone;
        String text = String.format(
                "Olá, %s! 👋 Bem-vindo(a) ao *ManejoPrime* — fazenda *%s*! 🐄\n\n" +
                "Você já pode registrar dados enviando um *áudio* aqui:\n\n" +
                "⚖️ *Pesagem* — \"Boi 45 pesou 380 kg\"\n" +
                "🩺 *Ocorrência* — \"Animal 12 está mancando\"\n" +
                "🌾 *Alimentação* — \"Alimentação do lote A, 200 kg\"\n" +
                "💉 *Vacinação* — \"Vacinei o animal 7 com aftosa\"\n" +
                "💊 *Medicação* — \"Apliquei Baymec no animal 3\"\n\n" +
                "Mande um áudio dizendo *\"ajuda\"* para ver essa lista de novo. 😊",
                displayName, farmName
        );

        try {
            outboundGateway.sendText(new WhatsappTextOutboundMessage(rawPhone, text, null));
        } catch (RuntimeException exception) {
            log.warn("Failed to send WhatsApp welcome message to {}: {}", rawPhone, exception.getMessage());
        }
    }
}
