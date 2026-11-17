package br.com.curralzap.backend.auth.application;

import br.com.curralzap.backend.users.domain.UserAccount;
import org.springframework.stereotype.Service;

@Service
public class LoginUserPolicy {

    public UserAccount requireActive(UserAccount userAccount) {
        if (!userAccount.active()) {
            throw new InactiveUserException();
        }

        return userAccount;
    }
}
