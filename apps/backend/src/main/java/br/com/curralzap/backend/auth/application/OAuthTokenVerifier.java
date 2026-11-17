package br.com.curralzap.backend.auth.application;

public interface OAuthTokenVerifier {

    boolean supports(String provider);

    OAuthIdentity verify(String idToken) throws OAuthTokenVerificationException;
}
