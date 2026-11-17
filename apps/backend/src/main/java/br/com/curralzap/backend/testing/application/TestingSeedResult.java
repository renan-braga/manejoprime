package br.com.curralzap.backend.testing.application;

import java.util.UUID;

public record TestingSeedResult(
        String accessToken,
        String refreshToken,
        UUID farmId,
        UUID userId,
        String phone
) {}
