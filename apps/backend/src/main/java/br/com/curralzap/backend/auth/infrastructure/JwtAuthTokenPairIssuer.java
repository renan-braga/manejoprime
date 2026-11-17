package br.com.curralzap.backend.auth.infrastructure;

import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.auth.application.AuthTokenParser;
import br.com.curralzap.backend.auth.application.AuthTokenPair;
import br.com.curralzap.backend.auth.application.AuthTokenPairIssuer;
import br.com.curralzap.backend.auth.application.AuthTokenType;
import br.com.curralzap.backend.auth.application.ExpiredAuthTokenException;
import br.com.curralzap.backend.auth.application.InvalidAuthTokenException;
import br.com.curralzap.backend.auth.application.OAuthIdentity;
import br.com.curralzap.backend.auth.application.OnboardingTokenClaims;
import br.com.curralzap.backend.auth.application.OnboardingTokenIssuer;
import br.com.curralzap.backend.auth.application.OnboardingTokenParser;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserRole;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
class JwtAuthTokenPairIssuer implements AuthTokenPairIssuer, AuthTokenParser, OnboardingTokenIssuer, OnboardingTokenParser {

    private static final String TOKEN_TYPE = "Bearer";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final byte[] signingKey;
    private final Duration accessTokenTtl;

    JwtAuthTokenPairIssuer(
            ObjectMapper objectMapper,
            @Value("${curralzap.auth.jwt.secret}") String signingSecret,
            @Value("${curralzap.auth.jwt.access-token-ttl:PT8H}") Duration accessTokenTtl
    ) {
        this.objectMapper = objectMapper;
        Assert.hasText(signingSecret, "curralzap.auth.jwt.secret must not be blank.");
        Assert.isTrue(!accessTokenTtl.isNegative() && !accessTokenTtl.isZero(), "curralzap.auth.jwt.access-token-ttl must be positive.");
        this.signingKey = signingSecret.getBytes(StandardCharsets.UTF_8);
        this.accessTokenTtl = accessTokenTtl;
    }

    @Override
    public String issue(OAuthIdentity identity, OffsetDateTime issuedAt, OffsetDateTime expiresAt) {
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", identity.email());
        payload.put("name", identity.name());
        payload.put("type", AuthTokenType.ONBOARDING.claimValue());
        payload.put("iat", issuedAt.toEpochSecond());
        payload.put("exp", expiresAt.toEpochSecond());
        try {
            String encodedHeader = encodeJson(header);
            String encodedPayload = encodeJson(payload);
            String signature = sign(encodedHeader + "." + encodedPayload);
            return encodedHeader + "." + encodedPayload + "." + signature;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize onboarding token payload.", exception);
        }
    }

    @Override
    public OnboardingTokenClaims verify(String token, OffsetDateTime now) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new InvalidAuthTokenException();
            }
            String content = parts[0] + "." + parts[1];
            String expectedSignature = sign(content);
            if (!MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8)
            )) {
                throw new InvalidAuthTokenException();
            }
            Map<String, Object> payload = objectMapper.readValue(
                    Base64.getUrlDecoder().decode(parts[1]),
                    MAP_TYPE
            );
            AuthTokenType tokenType = AuthTokenType.fromClaimValue(requiredString(payload, "type"));
            if (tokenType != AuthTokenType.ONBOARDING) {
                throw new InvalidAuthTokenException();
            }
            OffsetDateTime expiresAt = OffsetDateTime.ofInstant(
                    java.time.Instant.ofEpochSecond(requiredLong(payload, "exp")),
                    ZoneOffset.UTC
            );
            if (expiresAt.isBefore(now)) {
                throw new ExpiredAuthTokenException();
            }
            return new OnboardingTokenClaims(
                    requiredString(payload, "sub"),
                    requiredString(payload, "name"),
                    expiresAt
            );
        } catch (ExpiredAuthTokenException | InvalidAuthTokenException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new InvalidAuthTokenException("Unable to validate onboarding token.", exception);
        }
    }

    @Override
    public AuthTokenPair issueFor(
            UserAccount userAccount,
            UUID sessionId,
            UUID refreshTokenId,
            OffsetDateTime issuedAt,
            OffsetDateTime refreshExpiresAt
    ) {
        OffsetDateTime accessExpiresAt = issuedAt.plus(accessTokenTtl);

        return new AuthTokenPair(
                signToken(userAccount, sessionId, UUID.randomUUID(), issuedAt, accessExpiresAt, AuthTokenType.ACCESS),
                signToken(userAccount, sessionId, refreshTokenId, issuedAt, refreshExpiresAt, AuthTokenType.REFRESH),
                TOKEN_TYPE,
                accessExpiresAt,
                refreshExpiresAt
        );
    }

    @Override
    public AuthTokenClaims verify(String token, AuthTokenType expectedType, OffsetDateTime now) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new InvalidAuthTokenException();
            }

            String content = parts[0] + "." + parts[1];
            String expectedSignature = sign(content);
            if (!MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8)
            )) {
                throw new InvalidAuthTokenException();
            }

            Map<String, Object> payload = objectMapper.readValue(
                    Base64.getUrlDecoder().decode(parts[1]),
                    MAP_TYPE
            );

            AuthTokenType tokenType = AuthTokenType.fromClaimValue(requiredString(payload, "type"));
            if (tokenType != expectedType) {
                throw new InvalidAuthTokenException();
            }

            OffsetDateTime expiresAt = OffsetDateTime.ofInstant(
                    java.time.Instant.ofEpochSecond(requiredLong(payload, "exp")),
                    ZoneOffset.UTC
            );
            if (expiresAt.isBefore(now)) {
                throw new ExpiredAuthTokenException();
            }

            return new AuthTokenClaims(
                    UUID.fromString(requiredString(payload, "sub")),
                    UUID.fromString(requiredString(payload, "farmId")),
                    UserRole.fromDatabaseValue(requiredString(payload, "role")),
                    UUID.fromString(requiredString(payload, "sid")),
                    UUID.fromString(requiredString(payload, "jti")),
                    tokenType,
                    OffsetDateTime.ofInstant(
                            java.time.Instant.ofEpochSecond(requiredLong(payload, "iat")),
                            ZoneOffset.UTC
                    ),
                    expiresAt
            );
        } catch (ExpiredAuthTokenException exception) {
            throw exception;
        } catch (InvalidAuthTokenException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new InvalidAuthTokenException("Unable to validate auth token.", exception);
        }
    }

    private String signToken(
            UserAccount userAccount,
            UUID sessionId,
            UUID tokenId,
            OffsetDateTime issuedAt,
            OffsetDateTime expiresAt,
            AuthTokenType tokenType
    ) {
        Map<String, Object> header = Map.of(
                "alg", "HS256",
                "typ", "JWT"
        );

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", userAccount.id().toString());
        payload.put("farmId", userAccount.farmId().toString());
        payload.put("role", userAccount.role().name().toLowerCase());
        payload.put("type", tokenType.claimValue());
        payload.put("sid", sessionId.toString());
        payload.put("jti", tokenId.toString());
        payload.put("iat", issuedAt.toEpochSecond());
        payload.put("exp", expiresAt.toEpochSecond());

        try {
            String encodedHeader = encodeJson(header);
            String encodedPayload = encodeJson(payload);
            String signature = sign(encodedHeader + "." + encodedPayload);
            return encodedHeader + "." + encodedPayload + "." + signature;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize auth token payload.", exception);
        }
    }

    private String encodeJson(Map<String, Object> value) throws JsonProcessingException {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(value));
    }

    private String sign(String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign auth token.", exception);
        }
    }

    private String requiredString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (!(value instanceof String stringValue) || stringValue.isBlank()) {
            throw new InvalidAuthTokenException();
        }
        return stringValue;
    }

    private long requiredLong(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }
        throw new InvalidAuthTokenException();
    }
}
