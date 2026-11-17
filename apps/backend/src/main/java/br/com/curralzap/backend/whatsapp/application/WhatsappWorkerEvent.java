package br.com.curralzap.backend.whatsapp.application;

import java.util.UUID;

public interface WhatsappWorkerEvent {

    UUID inboundMessageId();

    String providerMessageId();

    String correlationId();

    String eventType();
}
