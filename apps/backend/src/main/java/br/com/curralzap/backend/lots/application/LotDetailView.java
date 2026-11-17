package br.com.curralzap.backend.lots.application;

import java.util.UUID;

public record LotDetailView(
        UUID id,
        UUID farmId,
        String name,
        String purpose,
        String pastureArea,
        long animalsCount
) {
}
