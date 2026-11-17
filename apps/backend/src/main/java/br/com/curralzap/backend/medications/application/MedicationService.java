package br.com.curralzap.backend.medications.application;

import br.com.curralzap.backend.animals.application.AnimalNotFoundException;
import br.com.curralzap.backend.animals.domain.AnimalRepository;
import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.auth.application.AuthenticationRequiredException;
import br.com.curralzap.backend.auth.application.LoginUserPolicy;
import br.com.curralzap.backend.auth.application.RolePermissionSummary;
import br.com.curralzap.backend.medications.domain.Medication;
import br.com.curralzap.backend.medications.domain.MedicationRepository;
import br.com.curralzap.backend.medications.domain.MedicationSource;
import br.com.curralzap.backend.occurrences.application.OccurrenceNotFoundException;
import br.com.curralzap.backend.occurrences.domain.Occurrence;
import br.com.curralzap.backend.occurrences.domain.OccurrenceRepository;
import br.com.curralzap.backend.shared.application.FarmScopedAccessGuard;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class MedicationService {

    private final UserAccountRepository userAccountRepository;
    private final LoginUserPolicy loginUserPolicy;
    private final AnimalRepository animalRepository;
    private final OccurrenceRepository occurrenceRepository;
    private final MedicationRepository medicationRepository;
    private final Clock clock;
    private final FarmScopedAccessGuard farmScopedAccessGuard;

    @Autowired
    public MedicationService(
            UserAccountRepository userAccountRepository,
            LoginUserPolicy loginUserPolicy,
            AnimalRepository animalRepository,
            OccurrenceRepository occurrenceRepository,
            MedicationRepository medicationRepository,
            FarmScopedAccessGuard farmScopedAccessGuard
    ) {
        this(
                userAccountRepository,
                loginUserPolicy,
                animalRepository,
                occurrenceRepository,
                medicationRepository,
                farmScopedAccessGuard,
                Clock.systemUTC()
        );
    }

    MedicationService(
            UserAccountRepository userAccountRepository,
            LoginUserPolicy loginUserPolicy,
            AnimalRepository animalRepository,
            OccurrenceRepository occurrenceRepository,
            MedicationRepository medicationRepository,
            FarmScopedAccessGuard farmScopedAccessGuard,
            Clock clock
    ) {
        this.userAccountRepository = userAccountRepository;
        this.loginUserPolicy = loginUserPolicy;
        this.animalRepository = animalRepository;
        this.occurrenceRepository = occurrenceRepository;
        this.medicationRepository = medicationRepository;
        this.farmScopedAccessGuard = farmScopedAccessGuard;
        this.clock = clock;
    }

    @Transactional
    public MedicationView create(AuthTokenClaims claims, MedicationCreateCommand command) {
        UserAccount activeUser = requireMedicationAccess(claims);

        farmScopedAccessGuard.requireInFarm(
                animalRepository,
                command.animalId(),
                claims.farmId(),
                AnimalNotFoundException::new
        );

        UUIDAndOccurrence occurrence = loadOccurrenceIfPresent(command, claims.farmId());
        if (occurrence != null && !occurrence.occurrence().animalId().equals(command.animalId())) {
            throw new OccurrenceNotFoundException();
        }

        Medication saved = medicationRepository.save(
                Medication.record(
                        claims.farmId(),
                        command.animalId(),
                        occurrence != null ? occurrence.id() : null,
                        activeUser.id(),
                        command.medicationName(),
                        command.activeIngredient(),
                        command.reason(),
                        command.dosage(),
                        command.applicationDate(),
                        command.withdrawalPeriodDays(),
                        command.cost(),
                        MedicationSource.APP
                )
        );

        return toView(saved);
    }

    @Transactional
    public void delete(AuthTokenClaims claims, UUID medicationId) {
        requireMedicationAccess(claims);
        medicationRepository.findByIdAndFarmId(medicationId, claims.farmId())
                .orElseThrow(MedicationNotFoundException::new);
        medicationRepository.delete(medicationId);
    }

    @Transactional(readOnly = true)
    public MedicationListView list(AuthTokenClaims claims, MedicationListQuery query) {
        requireMedicationAccess(claims);

        farmScopedAccessGuard.requireInFarm(
                animalRepository,
                query.animalId(),
                claims.farmId(),
                AnimalNotFoundException::new
        );

        List<Medication> history = medicationRepository.findAnimalHistory(claims.farmId(), query.animalId());
        int fromIndex = Math.min(query.offset(), history.size());
        int toIndex = Math.min(fromIndex + query.limit(), history.size());
        List<MedicationView> items = history.isEmpty()
                ? Collections.emptyList()
                : history.subList(fromIndex, toIndex).stream().map(this::toView).toList();

        return new MedicationListView(items, history.size(), query.limit(), query.offset());
    }

    private UUIDAndOccurrence loadOccurrenceIfPresent(MedicationCreateCommand command, java.util.UUID farmId) {
        if (command.occurrenceId() == null) {
            return null;
        }

        Occurrence occurrence = farmScopedAccessGuard.requireInFarm(
                occurrenceRepository,
                command.occurrenceId(),
                farmId,
                OccurrenceNotFoundException::new
        );
        return new UUIDAndOccurrence(command.occurrenceId(), occurrence);
    }

    private UserAccount requireMedicationAccess(AuthTokenClaims claims) {
        UserAccount userAccount = userAccountRepository.findById(claims.userId())
                .orElseThrow(AuthenticationRequiredException::new);
        UserAccount activeUser = loginUserPolicy.requireActive(userAccount);
        if (!RolePermissionSummary.permissionsFor(activeUser.role()).contains("medications.write")) {
            throw new MedicationAccessForbiddenException();
        }
        return activeUser;
    }

    private MedicationView toView(Medication medication) {
        LocalDate today = LocalDate.now(clock);
        return new MedicationView(
                medication.id(),
                medication.animalId(),
                medication.occurrenceId(),
                medication.medicationName(),
                medication.activeIngredient(),
                medication.reason(),
                medication.dosage(),
                medication.withdrawalPeriodDays(),
                medication.applicationDate(),
                medication.releaseDate(),
                medication.inWithdrawalPeriodOn(today),
                medication.cost(),
                medication.source().apiValue(),
                medication.recordedBy()
        );
    }

    private record UUIDAndOccurrence(java.util.UUID id, Occurrence occurrence) {
    }
}
