package br.com.curralzap.backend.reports.api;

import br.com.curralzap.backend.support.BackendApiIntegrationTestSupport;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserRole;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardApiIntegrationTest extends BackendApiIntegrationTestSupport {

    @Test
    void returnsFarmScopedDashboardSummaryForAdmin() throws Exception {
        UUID farmId = insertFarm("Fazenda Dashboard");
        UserAccount admin = insertUser(farmId, "5511999990100", "Dashboard Admin", UserRole.ADMIN, true);
        insertUser(farmId, "5511999990101", "Inactive Helper", UserRole.WORKER, false);
        UUID lotA = insertLot(farmId, "Engorda 1", "fattening");
        insertLot(farmId, "Cria", "raising");
        insertAnimal(farmId, lotA, "A-001", "ativo");
        insertAnimal(farmId, lotA, "A-002", "ativo");
        insertAnimal(farmId, lotA, "A-003", "vendido");
        insertAnimal(farmId, null, "A-004", "morto");

        java.util.UUID otherFarmId = insertFarm("Outra Fazenda");
        insertUser(otherFarmId, "5511999990199", "Other Worker", UserRole.WORKER, true);
        insertLot(otherFarmId, "Outro Lote", "breeding");
        insertAnimal(otherFarmId, null, "B-001", "transferido");

        String accessToken = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.totalAnimals").value(4))
                .andExpect(jsonPath("$.activeAnimals").value(2))
                .andExpect(jsonPath("$.lotsCount").value(2))
                .andExpect(jsonPath("$.activeUsers").value(1))
                .andExpect(jsonPath("$.statusBreakdown.active").value(2))
                .andExpect(jsonPath("$.statusBreakdown.sold").value(1))
                .andExpect(jsonPath("$.statusBreakdown.dead").value(1))
                .andExpect(jsonPath("$.statusBreakdown.transferred").value(0))
                .andExpect(jsonPath("$.alerts").isArray())
                .andExpect(jsonPath("$.alerts").isEmpty())
                .andExpect(jsonPath("$.shortcuts[0]").value("animals"))
                .andExpect(jsonPath("$.shortcuts[1]").value("users"))
                .andExpect(jsonPath("$.shortcuts[2]").value("settings"));
    }

    @Test
    void returnsWorkerShortcuts() throws Exception {
        UUID farmId = insertFarm("Fazenda Worker");
        UserAccount worker = insertUser(farmId, "5511999990200", "Worker Dashboard", UserRole.WORKER, true);
        String accessToken = issueAccessToken(worker, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortcuts[0]").value("weighing"))
                .andExpect(jsonPath("$.shortcuts[1]").value("occurrence"));
    }

    @Test
    void returnsVeterinarianShortcuts() throws Exception {
        UUID farmId = insertFarm("Fazenda Vet");
        UserAccount veterinarian = insertUser(farmId, "5511999990300", "Vet Dashboard", UserRole.VETERINARIAN, true);
        String accessToken = issueAccessToken(veterinarian, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortcuts[0]").value("occurrence"))
                .andExpect(jsonPath("$.shortcuts.length()").value(1));
    }

    @Test
    void returnsAuthenticationRequiredWhenHeaderIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_required"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void returnsTokenExpiredForExpiredAccessToken() throws Exception {
        UUID farmId = insertFarm("Fazenda Expirada");
        UserAccount userAccount = insertUser(farmId, "5511999990400", "Expired Dashboard", UserRole.ADMIN, true);
        OffsetDateTime issuedAt = OffsetDateTime.now(ZoneOffset.UTC).minusHours(9);
        String accessToken = issueAccessToken(userAccount, issuedAt);

        mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("token_expired"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void weightTrendReturnsAggregatedDailyAverages() throws Exception {
        UUID farmId = insertFarm("Fazenda Trend");
        UserAccount admin = insertUser(farmId, "5511999990500", "Trend Admin", UserRole.ADMIN, true);
        UUID lotId = insertLot(farmId, "Engorda Trend", "fattening");
        UUID animalA = insertAnimal(farmId, lotId, "T-001");
        UUID animalB = insertAnimal(farmId, lotId, "T-002");

        LocalDate today = LocalDate.now();
        insertWeighing(farmId, animalA, admin.id(), new BigDecimal("320.00"), today.minusDays(5));
        insertWeighing(farmId, animalB, admin.id(), new BigDecimal("360.00"), today.minusDays(5));
        insertWeighing(farmId, animalA, admin.id(), new BigDecimal("340.00"), today);

        UUID otherFarm = insertFarm("Outra Fazenda Trend");
        UserAccount otherAdmin = insertUser(otherFarm, "5511999990501", "Other Trend", UserRole.ADMIN, true);
        UUID otherAnimal = insertAnimal(otherFarm, null, "X-001");
        insertWeighing(otherFarm, otherAnimal, otherAdmin.id(), new BigDecimal("500.00"), today);

        String token = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/dashboard/weight-trend")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].avgWeight").value(340.00))
                .andExpect(jsonPath("$[1].avgWeight").value(340.00));
    }

    @Test
    void weightTrendReturnsEmptyListWhenNoWeighings() throws Exception {
        UUID farmId = insertFarm("Fazenda Sem Pesagens");
        UserAccount admin = insertUser(farmId, "5511999990510", "No Weigh Admin", UserRole.ADMIN, true);
        String token = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/dashboard/weight-trend")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void weightTrendRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/weight-trend"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_required"));
    }

    @Test
    void lotBreakdownReturnsHeadCountAndAvgWeightPerLot() throws Exception {
        UUID farmId = insertFarm("Fazenda Breakdown");
        UserAccount admin = insertUser(farmId, "5511999990600", "Breakdown Admin", UserRole.ADMIN, true);
        UUID lotA = insertLot(farmId, "Engorda A", "fattening");
        UUID lotB = insertLot(farmId, "Engorda B", "fattening");
        UUID animalA1 = insertAnimal(farmId, lotA, "B-001");
        UUID animalA2 = insertAnimal(farmId, lotA, "B-002");
        UUID animalB1 = insertAnimal(farmId, lotB, "B-003");

        LocalDate today = LocalDate.now();
        insertWeighing(farmId, animalA1, admin.id(), new BigDecimal("300.00"), today.minusDays(2));
        insertWeighing(farmId, animalA1, admin.id(), new BigDecimal("320.00"), today);
        insertWeighing(farmId, animalA2, admin.id(), new BigDecimal("280.00"), today);
        insertWeighing(farmId, animalB1, admin.id(), new BigDecimal("400.00"), today);

        String token = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/dashboard/lot-breakdown")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].lotName").value("Engorda A"))
                .andExpect(jsonPath("$[0].animalCount").value(2))
                .andExpect(jsonPath("$[0].avgWeight").value(300.00))
                .andExpect(jsonPath("$[1].lotName").value("Engorda B"))
                .andExpect(jsonPath("$[1].animalCount").value(1))
                .andExpect(jsonPath("$[1].avgWeight").value(400.00));
    }

    @Test
    void lotBreakdownExcludesOtherFarms() throws Exception {
        UUID farmId = insertFarm("Fazenda Isolada");
        UserAccount admin = insertUser(farmId, "5511999990610", "Isolada Admin", UserRole.ADMIN, true);
        UUID lotId = insertLot(farmId, "Lote Unico", "fattening");
        UUID animalId = insertAnimal(farmId, lotId, "I-001");
        insertWeighing(farmId, animalId, admin.id(), new BigDecimal("350.00"), LocalDate.now());

        UUID otherFarm = insertFarm("Outra Fazenda Isolada");
        UserAccount otherAdmin = insertUser(otherFarm, "5511999990611", "Other Isolada", UserRole.ADMIN, true);
        UUID otherLot = insertLot(otherFarm, "Lote Outro", "fattening");
        UUID otherAnimal = insertAnimal(otherFarm, otherLot, "J-001");
        insertWeighing(otherFarm, otherAnimal, otherAdmin.id(), new BigDecimal("999.00"), LocalDate.now());

        String token = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/dashboard/lot-breakdown")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].lotName").value("Lote Unico"));
    }

    @Test
    void lotBreakdownRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/lot-breakdown"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_required"));
    }

    private void insertAnimal(UUID farmId, UUID lotId, String identificacao, String status) {
        jdbcTemplate.update(
                """
                        insert into animals (
                            id, farm_id, lot_id, identificacao, sexo, status, data_entrada, created_at, updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                UUID.randomUUID(),
                farmId,
                lotId,
                identificacao,
                "macho",
                status,
                Date.valueOf(nowUtc().toLocalDate()),
                java.sql.Timestamp.from(nowUtc().toInstant()),
                java.sql.Timestamp.from(nowUtc().toInstant())
        );
    }
}
