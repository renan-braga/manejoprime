package br.com.curralzap.backend.vaccines.application;

import br.com.curralzap.backend.animals.application.AnimalNotFoundException;
import br.com.curralzap.backend.animals.domain.AnimalRepository;
import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.auth.application.AuthenticationRequiredException;
import br.com.curralzap.backend.auth.application.LoginUserPolicy;
import br.com.curralzap.backend.auth.application.RolePermissionSummary;
import br.com.curralzap.backend.lots.application.LotNotFoundException;
import br.com.curralzap.backend.lots.domain.LotRepository;
import br.com.curralzap.backend.shared.application.FarmScopedAccessGuard;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserAccountRepository;
import br.com.curralzap.backend.vaccines.domain.Vaccine;
import br.com.curralzap.backend.vaccines.domain.VaccineRepository;
import br.com.curralzap.backend.vaccines.domain.VaccineSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class VaccineService {

    private final UserAccountRepository userAccountRepository;
    private final LoginUserPolicy loginUserPolicy;
    private final AnimalRepository animalRepository;
    private final LotRepository lotRepository;
    private final VaccineRepository vaccineRepository;
    private final FarmScopedAccessGuard farmScopedAccessGuard;

    public VaccineService(
            UserAccountRepository userAccountRepository,
            LoginUserPolicy loginUserPolicy,
            AnimalRepository animalRepository,
            LotRepository lotRepository,
            VaccineRepository vaccineRepository,
            FarmScopedAccessGuard farmScopedAccessGuard
    ) {
        this.userAccountRepository = userAccountRepository;
        this.loginUserPolicy = loginUserPolicy;
        this.animalRepository = animalRepository;
        this.lotRepository = lotRepository;
        this.vaccineRepository = vaccineRepository;
        this.farmScopedAccessGuard = farmScopedAccessGuard;
    }

    @Transactional
    public VaccineView create(AuthTokenClaims claims, VaccineCreateCommand command) {
        UserAccount activeUser = requireVaccineAccess(claims);

        Vaccine saved = command.animalId() != null
                ? createAnimalVaccine(claims, activeUser, command)
                : createLotVaccine(claims, activeUser, command);

        return toView(saved);
    }

    @Transactional
    public void delete(AuthTokenClaims claims, UUID vaccineId) {
        requireVaccineAccess(claims);
        vaccineRepository.findByIdAndFarmId(vaccineId, claims.farmId())
                .orElseThrow(VaccineNotFoundException::new);
        vaccineRepository.delete(vaccineId);
    }

    @Transactional(readOnly = true)
    public VaccineListView list(AuthTokenClaims claims, VaccineListQuery query) {
        requireVaccineAccess(claims);

        List<Vaccine> history = query.animalTarget()
                ? listAnimalHistory(claims, query)
                : listLotHistory(claims, query);

        int fromIndex = Math.min(query.offset(), history.size());
        int toIndex = Math.min(fromIndex + query.limit(), history.size());
        List<VaccineView> items = history.isEmpty()
                ? Collections.emptyList()
                : history.subList(fromIndex, toIndex).stream().map(this::toView).toList();

        return new VaccineListView(items, history.size(), query.limit(), query.offset());
    }

    private Vaccine createAnimalVaccine(AuthTokenClaims claims, UserAccount activeUser, VaccineCreateCommand command) {
        farmScopedAccessGuard.requireInFarm(
                animalRepository,
                command.animalId(),
                claims.farmId(),
                AnimalNotFoundException::new
        );
        return vaccineRepository.save(
                Vaccine.recordForAnimal(
                        claims.farmId(),
                        command.animalId(),
                        activeUser.id(),
                        command.vaccineType(),
                        command.vaccineBatch(),
                        command.dose(),
                        command.applicationDate(),
                        command.nextDoseDate(),
                        command.volumeMl(),
                        VaccineSource.APP
                )
        );
    }

    private Vaccine createLotVaccine(AuthTokenClaims claims, UserAccount activeUser, VaccineCreateCommand command) {
        farmScopedAccessGuard.requireInFarm(
                lotRepository,
                command.lotId(),
                claims.farmId(),
                LotNotFoundException::new
        );
        return vaccineRepository.save(
                Vaccine.recordForLot(
                        claims.farmId(),
                        command.lotId(),
                        activeUser.id(),
                        command.vaccineType(),
                        command.vaccineBatch(),
                        command.dose(),
                        command.applicationDate(),
                        command.nextDoseDate(),
                        command.volumeMl(),
                        VaccineSource.APP
                )
        );
    }

    private List<Vaccine> listAnimalHistory(AuthTokenClaims claims, VaccineListQuery query) {
        farmScopedAccessGuard.requireInFarm(
                animalRepository,
                query.animalId(),
                claims.farmId(),
                AnimalNotFoundException::new
        );
        return vaccineRepository.findAnimalHistory(claims.farmId(), query.animalId());
    }

    private List<Vaccine> listLotHistory(AuthTokenClaims claims, VaccineListQuery query) {
        farmScopedAccessGuard.requireInFarm(
                lotRepository,
                query.lotId(),
                claims.farmId(),
                LotNotFoundException::new
        );
        return vaccineRepository.findLotHistory(claims.farmId(), query.lotId());
    }

    private UserAccount requireVaccineAccess(AuthTokenClaims claims) {
        UserAccount userAccount = userAccountRepository.findById(claims.userId())
                .orElseThrow(AuthenticationRequiredException::new);
        UserAccount activeUser = loginUserPolicy.requireActive(userAccount);
        if (!RolePermissionSummary.permissionsFor(activeUser.role()).contains("vaccines.write")) {
            throw new VaccineAccessForbiddenException();
        }
        return activeUser;
    }

    private VaccineView toView(Vaccine vaccine) {
        return new VaccineView(
                vaccine.id(),
                vaccine.animalId(),
                vaccine.lotId(),
                vaccine.vaccineType(),
                vaccine.vaccineBatch(),
                vaccine.dose().apiValue(),
                vaccine.applicationDate(),
                vaccine.nextDoseDate(),
                vaccine.volumeMl(),
                vaccine.source().apiValue(),
                vaccine.recordedBy()
        );
    }
}
