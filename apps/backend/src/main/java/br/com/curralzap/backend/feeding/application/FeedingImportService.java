package br.com.curralzap.backend.feeding.application;

import br.com.curralzap.backend.animals.domain.Animal;
import br.com.curralzap.backend.animals.domain.AnimalRepository;
import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.auth.application.AuthenticationRequiredException;
import br.com.curralzap.backend.auth.application.LoginUserPolicy;
import br.com.curralzap.backend.auth.application.RolePermissionSummary;
import br.com.curralzap.backend.feeding.domain.Feeding;
import br.com.curralzap.backend.feeding.domain.FeedingRepository;
import br.com.curralzap.backend.feeding.domain.FeedingSource;
import br.com.curralzap.backend.feeding.domain.FeedingType;
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

@Service
public class FeedingImportService {

    private final UserAccountRepository userAccountRepository;
    private final LoginUserPolicy loginUserPolicy;
    private final AnimalRepository animalRepository;
    private final LotRepository lotRepository;
    private final FeedingRepository feedingRepository;

    public FeedingImportService(
            UserAccountRepository userAccountRepository,
            LoginUserPolicy loginUserPolicy,
            AnimalRepository animalRepository,
            LotRepository lotRepository,
            FeedingRepository feedingRepository
    ) {
        this.userAccountRepository = userAccountRepository;
        this.loginUserPolicy = loginUserPolicy;
        this.animalRepository = animalRepository;
        this.lotRepository = lotRepository;
        this.feedingRepository = feedingRepository;
    }

    @Transactional
    public ImportResultView importFeedings(AuthTokenClaims claims, List<FeedingImportRow> rows) {
        requireWritableUser(claims);

        List<ImportRowError> errors = new ArrayList<>();
        int imported = 0;

        for (int i = 0; i < rows.size(); i++) {
            FeedingImportRow row = rows.get(i);
            int rowNumber = i + 1;

            String brinco = sanitize(row.brincoAnimal());
            String nomeLote = sanitize(row.nomeLote());
            boolean hasBrinco = brinco != null && !brinco.isBlank();
            boolean hasLote = nomeLote != null && !nomeLote.isBlank();

            if (hasBrinco == hasLote) {
                errors.add(new ImportRowError(rowNumber,
                        "Preencha exatamente um dos campos: brinco_animal ou nome_lote (não ambos e não nenhum)."));
                continue;
            }

            String tipoAlimento = sanitize(row.tipoAlimento());
            if (tipoAlimento == null || tipoAlimento.isBlank()) {
                errors.add(new ImportRowError(rowNumber, "tipo_alimento: campo obrigatório vazio."));
                continue;
            }

            FeedingType feedingType = parseFeedingType(tipoAlimento);
            if (feedingType == null) {
                errors.add(new ImportRowError(rowNumber,
                        "tipo_alimento: '" + tipoAlimento + "' não reconhecido. Use: pasto, racao, silagem, suplemento, sal_mineral, misto."));
                continue;
            }

            BigDecimal quantidadeKg = parseBigDecimal(row.quantidadeKg());
            if (quantidadeKg == null || quantidadeKg.signum() <= 0) {
                errors.add(new ImportRowError(rowNumber,
                        "quantidade_kg: campo obrigatório. Informe um número positivo (ex: 5.5)."));
                continue;
            }

            LocalDate dataInicio = parseDate(row.dataInicio());
            if (dataInicio == null) {
                errors.add(new ImportRowError(rowNumber,
                        "data_inicio: campo obrigatório. Use AAAA-MM-DD (ex: 2024-01-15)."));
                continue;
            }

            LocalDate dataFim = null;
            if (row.dataFim() != null && !row.dataFim().isBlank()) {
                dataFim = parseDate(row.dataFim());
                if (dataFim == null) {
                    errors.add(new ImportRowError(rowNumber,
                            "data_fim: '" + row.dataFim() + "' formato inválido. Use AAAA-MM-DD (ex: 2024-02-15)."));
                    continue;
                }
                if (dataFim.isBefore(dataInicio)) {
                    errors.add(new ImportRowError(rowNumber,
                            "data_fim: deve ser igual ou posterior à data_inicio."));
                    continue;
                }
            }

            BigDecimal custoPorKg = null;
            if (row.custoPorKg() != null && !row.custoPorKg().isBlank()) {
                custoPorKg = parseBigDecimal(row.custoPorKg());
                if (custoPorKg == null) {
                    errors.add(new ImportRowError(rowNumber, "custo_por_kg: valor numérico inválido."));
                    continue;
                }
            }

            String descricao = sanitize(row.descricao());

            if (hasBrinco) {
                Optional<Animal> animalOpt = animalRepository.findByFarmIdAndIdentifier(claims.farmId(), brinco);
                if (animalOpt.isEmpty()) {
                    errors.add(new ImportRowError(rowNumber,
                            "brinco_animal: '" + brinco + "' não encontrado — importe os animais primeiro."));
                    continue;
                }
                feedingRepository.save(Feeding.recordForAnimal(
                        claims.farmId(),
                        animalOpt.get().id(),
                        claims.userId(),
                        feedingType,
                        descricao,
                        quantidadeKg,
                        custoPorKg,
                        dataInicio,
                        dataFim,
                        FeedingSource.APP
                ));
            } else {
                Optional<Lot> lotOpt = lotRepository.findByFarmIdAndName(claims.farmId(), nomeLote);
                if (lotOpt.isEmpty()) {
                    errors.add(new ImportRowError(rowNumber,
                            "nome_lote: '" + nomeLote + "' não encontrado — importe os lotes primeiro."));
                    continue;
                }
                feedingRepository.save(Feeding.recordForLot(
                        claims.farmId(),
                        lotOpt.get().id(),
                        claims.userId(),
                        feedingType,
                        descricao,
                        quantidadeKg,
                        custoPorKg,
                        dataInicio,
                        dataFim,
                        FeedingSource.APP
                ));
            }
            imported++;
        }

        return new ImportResultView(imported, 0, errors);
    }

    private FeedingType parseFeedingType(String value) {
        if (value == null || value.isBlank()) return null;
        return switch (value.trim().toLowerCase()) {
            case "pasto", "pasture" -> FeedingType.PASTURE;
            case "racao", "ração", "feed" -> FeedingType.FEED;
            case "silagem", "silage" -> FeedingType.SILAGE;
            case "suplemento", "supplement" -> FeedingType.SUPPLEMENT;
            case "sal_mineral", "sal mineral", "mineral_salt" -> FeedingType.MINERAL_SALT;
            case "misto", "mixed" -> FeedingType.MIXED;
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

    private void requireWritableUser(AuthTokenClaims claims) {
        UserAccount userAccount = userAccountRepository.findById(claims.userId())
                .orElseThrow(AuthenticationRequiredException::new);
        UserAccount activeUser = loginUserPolicy.requireActive(userAccount);
        if (!RolePermissionSummary.permissionsFor(activeUser.role()).contains("feeding.write")) {
            throw new FeedingAccessForbiddenException();
        }
    }
}
