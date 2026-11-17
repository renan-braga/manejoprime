package br.com.curralzap.backend.vaccines.application;

import br.com.curralzap.backend.animals.domain.Animal;
import br.com.curralzap.backend.animals.domain.AnimalRepository;
import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.auth.application.AuthenticationRequiredException;
import br.com.curralzap.backend.auth.application.LoginUserPolicy;
import br.com.curralzap.backend.auth.application.RolePermissionSummary;
import br.com.curralzap.backend.shared.application.ImportResultView;
import br.com.curralzap.backend.shared.application.ImportResultView.ImportRowError;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserAccountRepository;
import br.com.curralzap.backend.vaccines.domain.Vaccine;
import br.com.curralzap.backend.vaccines.domain.VaccineDose;
import br.com.curralzap.backend.vaccines.domain.VaccineRepository;
import br.com.curralzap.backend.vaccines.domain.VaccineSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class VaccineImportService {

    private final UserAccountRepository userAccountRepository;
    private final LoginUserPolicy loginUserPolicy;
    private final AnimalRepository animalRepository;
    private final VaccineRepository vaccineRepository;

    public VaccineImportService(
            UserAccountRepository userAccountRepository,
            LoginUserPolicy loginUserPolicy,
            AnimalRepository animalRepository,
            VaccineRepository vaccineRepository
    ) {
        this.userAccountRepository = userAccountRepository;
        this.loginUserPolicy = loginUserPolicy;
        this.animalRepository = animalRepository;
        this.vaccineRepository = vaccineRepository;
    }

    @Transactional
    public ImportResultView importVaccines(AuthTokenClaims claims, List<VaccineImportRow> rows) {
        requireWritableUser(claims);

        List<ImportRowError> errors = new ArrayList<>();
        int imported = 0;

        for (int i = 0; i < rows.size(); i++) {
            VaccineImportRow row = rows.get(i);
            int rowNumber = i + 1;

            String brinco = sanitize(row.brincoAnimal());
            if (brinco == null || brinco.isBlank()) {
                errors.add(new ImportRowError(rowNumber, "brinco_animal: campo obrigatório vazio."));
                continue;
            }

            Optional<Animal> animalOpt = animalRepository.findByFarmIdAndIdentifier(claims.farmId(), brinco);
            if (animalOpt.isEmpty()) {
                errors.add(new ImportRowError(rowNumber,
                        "brinco_animal: '" + brinco + "' não encontrado — importe os animais primeiro."));
                continue;
            }

            String tipoVacina = sanitize(row.tipoVacina());
            if (tipoVacina == null || tipoVacina.isBlank()) {
                errors.add(new ImportRowError(rowNumber, "tipo_vacina: campo obrigatório vazio."));
                continue;
            }
            if (tipoVacina.length() > 100) {
                errors.add(new ImportRowError(rowNumber, "tipo_vacina: deve ter no máximo 100 caracteres."));
                continue;
            }
            if (containsHtml(tipoVacina)) {
                errors.add(new ImportRowError(rowNumber, "tipo_vacina: contém caracteres inválidos (< > &)."));
                continue;
            }

            VaccineDose dose = parseDose(row.dose());
            if (dose == null) {
                errors.add(new ImportRowError(rowNumber,
                        "dose: '" + row.dose() + "' não reconhecida. Use: D1, D2, reforco ou anual."));
                continue;
            }

            LocalDate applicationDate = parseDate(row.dataAplicacao());
            if (applicationDate == null) {
                errors.add(new ImportRowError(rowNumber,
                        "data_aplicacao: campo obrigatório. Use AAAA-MM-DD (ex: 2024-01-15)."));
                continue;
            }

            LocalDate nextDoseDate = parseDate(row.proximaDose());
            if (row.proximaDose() != null && !row.proximaDose().isBlank() && nextDoseDate == null) {
                errors.add(new ImportRowError(rowNumber,
                        "proxima_dose: '" + row.proximaDose() + "' formato inválido. Use AAAA-MM-DD (ex: 2024-07-15)."));
                continue;
            }

            BigDecimal volumeMl = parseBigDecimal(row.volumeMl());
            if (row.volumeMl() != null && !row.volumeMl().isBlank() && volumeMl == null) {
                errors.add(new ImportRowError(rowNumber, "volume_ml: valor numérico inválido."));
                continue;
            }

            String loteVacina = sanitize(row.loteVacina());
            if (loteVacina != null && loteVacina.length() > 100) {
                errors.add(new ImportRowError(rowNumber, "lote_vacina: deve ter no máximo 100 caracteres."));
                continue;
            }

            vaccineRepository.save(Vaccine.recordForAnimal(
                    claims.farmId(),
                    animalOpt.get().id(),
                    claims.userId(),
                    tipoVacina,
                    loteVacina,
                    dose,
                    applicationDate,
                    nextDoseDate,
                    volumeMl,
                    VaccineSource.APP
            ));
            imported++;
        }

        return new ImportResultView(imported, 0, errors);
    }

    private VaccineDose parseDose(String value) {
        if (value == null || value.isBlank()) return null;
        return switch (value.trim().toLowerCase()) {
            case "d1", "primeira", "first" -> VaccineDose.FIRST;
            case "d2", "segunda", "second" -> VaccineDose.SECOND;
            case "reforco", "reforço", "booster" -> VaccineDose.BOOSTER;
            case "anual", "annual" -> VaccineDose.ANNUAL;
            default -> null;
        };
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return new BigDecimal(value.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
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
        if (!RolePermissionSummary.permissionsFor(activeUser.role()).contains("vaccines.write")) {
            throw new VaccineAccessForbiddenException();
        }
    }
}
