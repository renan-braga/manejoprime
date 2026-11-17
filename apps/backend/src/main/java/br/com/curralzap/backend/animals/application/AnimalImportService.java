package br.com.curralzap.backend.animals.application;

import br.com.curralzap.backend.animals.domain.Animal;
import br.com.curralzap.backend.animals.domain.AnimalRepository;
import br.com.curralzap.backend.animals.domain.AnimalSex;
import br.com.curralzap.backend.animals.domain.AnimalStatus;
import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.auth.application.AuthenticationRequiredException;
import br.com.curralzap.backend.auth.application.LoginUserPolicy;
import br.com.curralzap.backend.auth.application.RolePermissionSummary;
import br.com.curralzap.backend.billing.application.AnimalQuotaPolicy;
import br.com.curralzap.backend.lots.domain.Lot;
import br.com.curralzap.backend.lots.domain.LotRepository;
import br.com.curralzap.backend.shared.application.ImportResultView;
import br.com.curralzap.backend.shared.application.ImportResultView.ImportRowError;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AnimalImportService {

    private final UserAccountRepository userAccountRepository;
    private final LoginUserPolicy loginUserPolicy;
    private final AnimalRepository animalRepository;
    private final LotRepository lotRepository;
    private final AnimalQuotaPolicy animalQuotaPolicy;

    public AnimalImportService(
            UserAccountRepository userAccountRepository,
            LoginUserPolicy loginUserPolicy,
            AnimalRepository animalRepository,
            LotRepository lotRepository,
            AnimalQuotaPolicy animalQuotaPolicy
    ) {
        this.userAccountRepository = userAccountRepository;
        this.loginUserPolicy = loginUserPolicy;
        this.animalRepository = animalRepository;
        this.lotRepository = lotRepository;
        this.animalQuotaPolicy = animalQuotaPolicy;
    }

    @Transactional
    public ImportResultView importAnimals(AuthTokenClaims claims, List<AnimalImportRow> rows) {
        requireWritableUser(claims);

        // Quota check before persisting anything
        long existingCount = animalRepository.countByFarmId(claims.farmId());
        int newAnimals = countDistinctNewBrincos(claims.farmId(), rows);
        animalQuotaPolicy.requireQuotaAvailable(claims.farmId(), newAnimals);

        List<ImportRowError> errors = new ArrayList<>();
        int imported = 0;
        int skipped = 0;

        for (int i = 0; i < rows.size(); i++) {
            AnimalImportRow row = rows.get(i);
            int rowNumber = i + 1;

            String brinco = sanitize(row.brinco());
            if (brinco == null || brinco.isBlank()) {
                errors.add(new ImportRowError(rowNumber, "brinco: campo obrigatório vazio."));
                continue;
            }
            if (brinco.length() > 50) {
                errors.add(new ImportRowError(rowNumber, "brinco: deve ter no máximo 50 caracteres."));
                continue;
            }
            if (containsHtml(brinco)) {
                errors.add(new ImportRowError(rowNumber, "brinco: contém caracteres inválidos (< > &)."));
                continue;
            }

            if (animalRepository.existsByFarmIdAndIdentifier(claims.farmId(), brinco)) {
                skipped++;
                continue;
            }

            AnimalSex sex = parseSex(row.sexo());
            if (sex == null) {
                errors.add(new ImportRowError(rowNumber,
                        "sexo: '" + row.sexo() + "' não reconhecido. Use 'macho' ou 'femea'."));
                continue;
            }

            AnimalStatus status = parseStatus(row.status());
            if (status == null) {
                errors.add(new ImportRowError(rowNumber,
                        "status: '" + row.status() + "' não reconhecido. Use: ativo, vendido, morto ou transferido."));
                continue;
            }

            LocalDate birthDate = parseDate(row.dataNascimento());
            if (row.dataNascimento() != null && !row.dataNascimento().isBlank() && birthDate == null) {
                errors.add(new ImportRowError(rowNumber,
                        "data_nascimento: '" + row.dataNascimento() + "' formato inválido. Use AAAA-MM-DD (ex: 2020-01-15)."));
                continue;
            }

            LocalDate entryDate = parseDate(row.dataEntrada());
            if (row.dataEntrada() != null && !row.dataEntrada().isBlank() && entryDate == null) {
                errors.add(new ImportRowError(rowNumber,
                        "data_entrada: '" + row.dataEntrada() + "' formato inválido. Use AAAA-MM-DD (ex: 2021-03-01)."));
                continue;
            }

            BigDecimal birthWeight = parseBigDecimal(row.pesoNascimentoKg());
            if (row.pesoNascimentoKg() != null && !row.pesoNascimentoKg().isBlank() && birthWeight == null) {
                errors.add(new ImportRowError(rowNumber, "peso_nascimento_kg: valor numérico inválido."));
                continue;
            }

            UUID lotId = null;
            String nomeLote = sanitize(row.nomeLote());
            if (nomeLote != null && !nomeLote.isBlank()) {
                Optional<Lot> lot = lotRepository.findByFarmIdAndName(claims.farmId(), nomeLote);
                if (lot.isEmpty()) {
                    errors.add(new ImportRowError(rowNumber,
                            "nome_lote: '" + nomeLote + "' não encontrado — importe os lotes primeiro."));
                    continue;
                }
                lotId = lot.get().id();
            }

            String nome = sanitize(row.nome());
            String raca = sanitize(row.raca());
            if (nome != null && nome.length() > 100) {
                errors.add(new ImportRowError(rowNumber, "nome: deve ter no máximo 100 caracteres."));
                continue;
            }
            if (raca != null && raca.length() > 100) {
                errors.add(new ImportRowError(rowNumber, "raca: deve ter no máximo 100 caracteres."));
                continue;
            }

            AnimalMutationCommand command = new AnimalMutationCommand(
                    brinco, nome, raca, sex, birthDate, birthWeight, status, entryDate, lotId
            );
            animalRepository.save(Animal.create(claims.farmId(), command));
            imported++;
        }

        return new ImportResultView(imported, skipped, errors);
    }

    private int countDistinctNewBrincos(UUID farmId, List<AnimalImportRow> rows) {
        return (int) rows.stream()
                .map(r -> sanitize(r.brinco()))
                .filter(b -> b != null && !b.isBlank())
                .filter(b -> !animalRepository.existsByFarmIdAndIdentifier(farmId, b))
                .distinct()
                .count();
    }

    private AnimalSex parseSex(String value) {
        if (value == null || value.isBlank()) return null;
        return switch (value.trim().toLowerCase()) {
            case "macho", "male", "m" -> AnimalSex.MALE;
            case "femea", "fêmea", "female", "f" -> AnimalSex.FEMALE;
            default -> null;
        };
    }

    private AnimalStatus parseStatus(String value) {
        if (value == null || value.isBlank()) return AnimalStatus.ACTIVE;
        return switch (value.trim().toLowerCase()) {
            case "ativo", "active" -> AnimalStatus.ACTIVE;
            case "vendido", "sold" -> AnimalStatus.SOLD;
            case "morto", "dead" -> AnimalStatus.DEAD;
            case "transferido", "transferred" -> AnimalStatus.TRANSFERRED;
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
        if (!RolePermissionSummary.permissionsFor(activeUser.role()).contains("animals.write")) {
            throw new AnimalWriteForbiddenException();
        }
    }
}
