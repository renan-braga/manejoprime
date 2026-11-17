package br.com.curralzap.backend.whatsapp.application;

import java.util.List;

public record WhatsappSupportMessageListView(
        List<WhatsappSupportMessageListItemView> items,
        long total,
        int limit,
        int offset
) {
}
