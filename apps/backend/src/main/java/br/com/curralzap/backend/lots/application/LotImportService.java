package br.com.curralzap.backend.lots.application;

import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.auth.application.AuthenticationRequiredException;
import br.com.curralzap.backend.auth.application.LoginUserPolicy;
import br.com.curralzap.backend.auth.application.RolePermissionSummary;
import br.com.curralzap.backend.lots.domain.Lot;
import br.com.curralzap.backend.lots.domain.LotPurpose;
import br.com.curralzap.backend.lots.domain.LotRepository;
import br.com.curralzap.backend.shared.application.ImportResultView;
import br.com.curralzap.backend.shared.application.ImportResultView.ImportRowError;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class LotImportService {

    private static final Map<String, LotPurpose> PURPOSE_MAP = Map.of(
            "engorda", LotPurpose.FATTENING,
            "fattening", LotPurpose.FATTENING,
            "recria", LotPurpose.RAISING,
            "raising", LotPurpose.RAISING,
            "cria", LotPurpose.BREEDING,
            "breeding", LotPurpose.BREEDING,
            "reproducao", LotPurpose.REPRODUCTION,
            "reproduction", LotPurpose.REPRODUCTION
    );

    private final UserAccountRepository userAccountRepository;
    private final LoginUserPolicy loginUserPolicy;
    private final LotRepository lotRepository;

    public LotImportService(
            UserAccountRepository userAccountRepository,
            LoginUserPolicy loginUserPolicy,
            LotRepository lotRepository
    ) {
        this.userAccountRepository = userAccountRepository;
        this.loginUserPolicy = loginUserPolicy;
        this.lotRepository = lotRepository;
    }

    @Transactional
    public ImportResultView importLots(AuthTokenClaims claims, List<LotImportRow> rows) {
        requireWritableUser(claims);

        List<ImportRowError> errors = new ArrayList<>();
        int imported = 0;
        int skipped = 0;

        for (int i = 0; i < rows.size(); i++) {
            LotImportRow row = rows.get(i);
            int rowNumber = i + 1;

            String name = sanitize(row.name());
            if (name == null || name.isBlank()) {
                errors.add(new ImportRowError(rowNumber, "nome: campo obrigatório vazio."));
                continue;
            }
            if (name.length() > 100) {
                errors.add(new ImportRowError(rowNumber, "nome: deve ter no máximo 100 caracteres."));
                continue;
            }
            if (containsHtml(name)) {
                errors.add(new ImportRowError(rowNumber, "nome: contém caracteres inválidos (< > &)."));
                continue;
            }

            LotPurpose purpose = parsePurpose(row.purpose());
            if (purpose == null) {
                errors.add(new ImportRowError(rowNumber,
                        "finalidade: '" + row.purpose() + "' não reconhecida. Use: engorda, recria, cria ou reproducao."));
                continue;
            }

            String pastureArea = sanitize(row.pastureArea());
            if (pastureArea != null && pastureArea.length() > 100) {
                errors.add(new ImportRowError(rowNumber, "area_pasto: deve ter no máximo 100 caracteres."));
                continue;
            }

            if (lotRepository.existsByFarmIdAndName(claims.farmId(), name)) {
                skipped++;
                continue;
            }

            lotRepository.save(Lot.create(claims.farmId(), new LotUpsertInput(name, purpose, pastureArea)));
            imported++;
        }

        return new ImportResultView(imported, skipped, errors);
    }

    private LotPurpose parsePurpose(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toLowerCase()
                .replace("ç", "c").replace("ã", "a");
        return PURPOSE_MAP.get(normalized);
    }

    private String sanitize(String value) {
        if (value == null) return null;
        return value.trim().replace("\0", "");
    }

    private boolean containsHtml(String value) {
        return value.contains("<") || value.contains(">") || value.contains("&");
    }

    private void requireWritableUser(AuthTokenClaims claims) {
        UserAccount userAccount = userAccountRepository.findById(claims.userId())
                .orElseThrow(AuthenticationRequiredException::new);
        UserAccount activeUser = loginUserPolicy.requireActive(userAccount);
        if (!RolePermissionSummary.permissionsFor(activeUser.role()).contains("lots.write")) {
            throw new LotWriteForbiddenException();
        }
    }
}
