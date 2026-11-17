package br.com.curralzap.backend.users.application;

import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.auth.application.AuthenticationRequiredException;
import br.com.curralzap.backend.auth.application.LoginUserPolicy;
import br.com.curralzap.backend.auth.domain.InvalidPhoneFormatException;
import br.com.curralzap.backend.auth.domain.PhoneNumber;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserAccountRepository;
import br.com.curralzap.backend.whatsapp.application.WhatsappWelcomeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class ProfileService {

    private final UserAccountRepository userAccountRepository;
    private final LoginUserPolicy loginUserPolicy;
    private final WhatsappWelcomeService whatsappWelcomeService;

    ProfileService(
            UserAccountRepository userAccountRepository,
            LoginUserPolicy loginUserPolicy,
            WhatsappWelcomeService whatsappWelcomeService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.loginUserPolicy = loginUserPolicy;
        this.whatsappWelcomeService = whatsappWelcomeService;
    }

    @Transactional
    public UserDetailView updatePhone(AuthTokenClaims claims, String phone) {
        UserAccount current = userAccountRepository.findById(claims.userId())
                .orElseThrow(AuthenticationRequiredException::new);
        loginUserPolicy.requireActive(current);

        if (phone == null || phone.isBlank()) {
            return new UserDetailView(current.id(), current.farmId(), current.name(), current.phone(), current.role().databaseValue(), current.active());
        }

        String normalizedPhone = parsePhone(phone);
        boolean phoneChanged = !Objects.equals(current.phone(), normalizedPhone);
        UserAccount saved = userAccountRepository.save(current.withPhone(normalizedPhone));

        if (phoneChanged && normalizedPhone != null) {
            whatsappWelcomeService.sendWelcome(normalizedPhone, saved.name(), saved.farmId());
        }

        return new UserDetailView(saved.id(), saved.farmId(), saved.name(), saved.phone(), saved.role().databaseValue(), saved.active());
    }

    private String parsePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        try {
            return PhoneNumber.parse(phone).normalized();
        } catch (InvalidPhoneFormatException exception) {
            fieldErrors.put("phone", exception.getMessage());
            throw new InvalidUserMutationException(fieldErrors);
        }
    }
}
