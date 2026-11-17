package br.com.curralzap.backend.farms.application;

import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.auth.application.AuthenticationRequiredException;
import br.com.curralzap.backend.auth.application.LoginUserPolicy;
import br.com.curralzap.backend.auth.application.RolePermissionSummary;
import br.com.curralzap.backend.farms.domain.FarmSettings;
import br.com.curralzap.backend.farms.domain.FarmSettingsRepository;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FarmSettingsService {

    private final UserAccountRepository userAccountRepository;
    private final LoginUserPolicy loginUserPolicy;
    private final FarmSettingsRepository farmSettingsRepository;

    FarmSettingsService(
            UserAccountRepository userAccountRepository,
            LoginUserPolicy loginUserPolicy,
            FarmSettingsRepository farmSettingsRepository
    ) {
        this.userAccountRepository = userAccountRepository;
        this.loginUserPolicy = loginUserPolicy;
        this.farmSettingsRepository = farmSettingsRepository;
    }

    public FarmSettingsView get(AuthTokenClaims claims) {
        requirePermission(claims, "settings.read");
        FarmSettings settings = farmSettingsRepository.findByFarmId(claims.farmId())
                .orElseThrow(FarmSettingsNotFoundException::new);
        return toView(settings);
    }

    @Transactional
    public FarmSettingsView update(AuthTokenClaims claims, FarmSettingsInput input) {
        requirePermission(claims, "settings.write");
        FarmSettings existingSettings = farmSettingsRepository.findByFarmId(claims.farmId())
                .orElseThrow(FarmSettingsNotFoundException::new);
        return toView(farmSettingsRepository.save(existingSettings.update(input)));
    }

    private FarmSettingsView toView(FarmSettings settings) {
        return new FarmSettingsView(
                settings.farmId(),
                settings.farmName(),
                settings.carcassYieldPercent()
        );
    }

    private void requirePermission(AuthTokenClaims claims, String permission) {
        UserAccount activeUser = userAccountRepository.findById(claims.userId())
                .orElseThrow(AuthenticationRequiredException::new);
        activeUser = loginUserPolicy.requireActive(activeUser);
        if (!RolePermissionSummary.permissionsFor(activeUser.role()).contains(permission)) {
            throw new FarmSettingsForbiddenException();
        }
    }
}
