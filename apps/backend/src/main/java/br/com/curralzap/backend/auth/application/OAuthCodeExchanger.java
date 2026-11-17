package br.com.curralzap.backend.auth.application;

public interface OAuthCodeExchanger {

    boolean supports(String provider);

    String exchangeForIdToken(String code, String codeVerifier, String redirectUri) throws OAuthTokenVerificationException;
}
