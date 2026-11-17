package br.com.curralzap.backend.whatsapp.application;

import java.util.UUID;

public interface WhatsappSupportReadViewRepository {

    WhatsappSupportMessageListView listFor(UUID farmId, UUID userId, WhatsappSupportMessageQuery query);
}
