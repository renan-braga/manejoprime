package br.com.curralzap.backend.auth.application;

public sealed interface OAuthCallbackResult {
    record Authenticated(AuthTokenPair tokenPair) implements OAuthCallbackResult {}
    record OnboardingRequired(String onboardingToken) implements OAuthCallbackResult {}
}
