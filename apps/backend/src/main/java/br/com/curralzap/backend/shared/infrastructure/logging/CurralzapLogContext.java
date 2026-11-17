package br.com.curralzap.backend.shared.infrastructure.logging;

import br.com.curralzap.backend.whatsapp.application.WhatsappWorkerEvent;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CurralzapLogContext {

    public static final String REQUEST_ID = "requestId";
    public static final String CORRELATION_ID = "correlationId";
    public static final String INBOUND_MESSAGE_ID = "inboundMessageId";
    public static final String PROVIDER_MESSAGE_ID = "providerMessageId";
    public static final String WHATSAPP_EVENT_TYPE = "whatsappEventType";
    public static final String KAFKA_TOPIC = "kafkaTopic";
    public static final String KAFKA_GROUP_ID = "kafkaGroupId";

    private CurralzapLogContext() {
    }

    public static LogContextScope withRequestId(String requestId) {
        return LogContextScope.with(Map.of(REQUEST_ID, requestId));
    }

    public static LogContextScope withWhatsappEvent(
            WhatsappWorkerEvent event,
            String kafkaTopic,
            String kafkaGroupId
    ) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(CORRELATION_ID, event.correlationId());
        values.put(INBOUND_MESSAGE_ID, event.inboundMessageId() == null ? null : event.inboundMessageId().toString());
        values.put(PROVIDER_MESSAGE_ID, event.providerMessageId());
        values.put(WHATSAPP_EVENT_TYPE, event.eventType());
        values.put(KAFKA_TOPIC, kafkaTopic);
        values.put(KAFKA_GROUP_ID, kafkaGroupId);
        return LogContextScope.with(values);
    }
}
