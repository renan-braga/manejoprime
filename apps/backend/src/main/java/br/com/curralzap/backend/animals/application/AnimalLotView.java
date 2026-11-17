package br.com.curralzap.backend.animals.application;

import java.util.UUID;

public record AnimalLotView(
        UUID id,
        String name
) {
}
