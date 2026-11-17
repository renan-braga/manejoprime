package br.com.curralzap.backend.farms.application;

import br.com.curralzap.backend.auth.application.AuthSessionLifecycle;
import br.com.curralzap.backend.auth.application.AuthTokenPair;
import br.com.curralzap.backend.auth.application.OnboardingTokenClaims;
import br.com.curralzap.backend.auth.application.OnboardingTokenParser;
import br.com.curralzap.backend.farms.domain.Farm;
import br.com.curralzap.backend.farms.domain.FarmRepository;
import br.com.curralzap.backend.farms.domain.UserFarmMembershipRepository;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserAccountRepository;
import br.com.curralzap.backend.users.domain.UserRole;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class FarmRegistrationService {

    private final FarmRepository farmRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserFarmMembershipRepository membershipRepository;
    private final OnboardingTokenParser onboardingTokenParser;
    private final AuthSessionLifecycle authSessionLifecycle;

    public FarmRegistrationService(
            FarmRepository farmRepository,
            UserAccountRepository userAccountRepository,
            UserFarmMembershipRepository membershipRepository,
            OnboardingTokenParser onboardingTokenParser,
            AuthSessionLifecycle authSessionLifecycle
    ) {
        this.farmRepository = farmRepository;
        this.userAccountRepository = userAccountRepository;
        this.membershipRepository = membershipRepository;
        this.onboardingTokenParser = onboardingTokenParser;
        this.authSessionLifecycle = authSessionLifecycle;
    }

    @Transactional
    public AuthTokenPair register(String onboardingToken, String farmName) {
        validateFarmName(farmName);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OnboardingTokenClaims claims = onboardingTokenParser.verify(onboardingToken, now);

        Optional<UserAccount> existing = userAccountRepository.findByEmail(claims.email());
        if (existing.isPresent()) {
            return authSessionLifecycle.startSession(existing.get());
        }

        try {
            Farm farm = farmRepository.save(Farm.create(farmName));
            UserAccount user = userAccountRepository.save(
                    UserAccount.create(farm.id(), null, claims.email(), claims.name(), UserRole.ADMIN)
            );
            farmRepository.setOwner(farm.id(), user.id());
            membershipRepository.save(user.id(), farm.id(), "admin");
            return authSessionLifecycle.startSession(user);
        } catch (DataIntegrityViolationException exception) {
            UserAccount user = userAccountRepository.findByEmail(claims.email())
                    .orElseThrow(() -> exception);
            return authSessionLifecycle.startSession(user);
        }
    }

    private void validateFarmName(String farmName) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        if (farmName == null || farmName.isBlank()) {
            fieldErrors.put("farmName", "Nome da fazenda é obrigatório.");
        } else if (farmName.length() > 255) {
            fieldErrors.put("farmName", "Nome da fazenda deve ter no máximo 255 caracteres.");
        }
        if (!fieldErrors.isEmpty()) {
            throw new InvalidFarmRegistrationException(fieldErrors);
        }
    }
}
