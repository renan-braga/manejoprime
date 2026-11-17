package br.com.curralzap.backend.testing.application;

import br.com.curralzap.backend.auth.application.AuthSessionLifecycle;
import br.com.curralzap.backend.auth.application.AuthTokenPair;
import br.com.curralzap.backend.farms.domain.Farm;
import br.com.curralzap.backend.farms.domain.FarmRepository;
import br.com.curralzap.backend.farms.domain.UserFarmMembershipRepository;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserAccountRepository;
import br.com.curralzap.backend.users.domain.UserRole;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class TestingSeedService {

    private final FarmRepository farmRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserFarmMembershipRepository membershipRepository;
    private final AuthSessionLifecycle authSessionLifecycle;
    private final TestingDataRepository testingDataRepository;
    private final EntityManager entityManager;

    public TestingSeedService(
            FarmRepository farmRepository,
            UserAccountRepository userAccountRepository,
            UserFarmMembershipRepository membershipRepository,
            AuthSessionLifecycle authSessionLifecycle,
            TestingDataRepository testingDataRepository,
            EntityManager entityManager
    ) {
        this.farmRepository = farmRepository;
        this.userAccountRepository = userAccountRepository;
        this.membershipRepository = membershipRepository;
        this.authSessionLifecycle = authSessionLifecycle;
        this.testingDataRepository = testingDataRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public TestingSeedResult seed() {
        String runId = UUID.randomUUID().toString().substring(0, 8);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        Farm farm = farmRepository.save(Farm.create("E2E Test Farm " + runId));
        String testPhone = "5500" + System.currentTimeMillis() % 100_000_000L;
        String testEmail = "e2e+" + runId + "@test.manejoprime.com.br";

        UserAccount user = userAccountRepository.save(
                UserAccount.create(farm.id(), testPhone, testEmail, "E2E Test User", UserRole.ADMIN)
        );
        // Flush JPA insert to DB before the raw JDBC setOwner UPDATE checks the FK
        entityManager.flush();
        farmRepository.setOwner(farm.id(), user.id());
        membershipRepository.save(user.id(), farm.id(), "admin");

        testingDataRepository.insertActiveSubscription(farm.id(), now);

        AuthTokenPair tokens = authSessionLifecycle.startSession(user);
        return new TestingSeedResult(
                tokens.accessToken(),
                tokens.refreshToken(),
                farm.id(),
                user.id(),
                testPhone
        );
    }

    @Transactional
    public void teardown(UUID farmId) {
        testingDataRepository.deleteAllForFarm(farmId);
    }
}
