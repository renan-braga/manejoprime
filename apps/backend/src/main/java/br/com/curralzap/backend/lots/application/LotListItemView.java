package br.com.curralzap.backend.lots.application;

import java.util.UUID;

public record LotListItemView(
        UUID id,
        String name,
        String purpose,
        String pastureArea,
        long animalsCount
) {
}
