package br.com.curralzap.backend.auth.application;

import br.com.curralzap.backend.users.domain.UserAccount;

public interface AuthSessionLifecycle {

    AuthTokenPair startSession(UserAccount userAccount);

    AuthTokenPair refresh(String refreshToken);

    void logout(String refreshToken);
}
