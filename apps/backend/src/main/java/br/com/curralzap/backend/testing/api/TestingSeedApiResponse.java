package br.com.curralzap.backend.testing.api;

import br.com.curralzap.backend.testing.application.TestingSeedResult;

import java.util.UUID;

record TestingSeedApiResponse(
        String accessToken,
        String refreshToken,
        UUID farmId,
        UUID userId,
        String phone
) {
    static TestingSeedApiResponse from(TestingSeedResult result) {
        return new TestingSeedApiResponse(
                result.accessToken(),
                result.refreshToken(),
                result.farmId(),
                result.userId(),
                result.phone()
        );
    }
}
