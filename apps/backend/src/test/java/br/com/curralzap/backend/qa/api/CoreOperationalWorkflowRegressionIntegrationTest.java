package br.com.curralzap.backend.qa.api;

import br.com.curralzap.backend.support.BackendApiIntegrationTestSupport;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserRole;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CoreOperationalWorkflowRegressionIntegrationTest extends BackendApiIntegrationTestSupport {

    @Test
    void weighingCreationRemainsIdempotentAndComputesDailyGainAgainstPriorHistory() throws Exception {
        UUID farmId = insertFarm("Fazenda Regressao Pesagem");
        UUID animalId = insertAnimal(farmId, null, "QA-006-W-001");
        UserAccount worker = insertUser(farmId, "5511999999601", "Worker Pesagem", UserRole.WORKER, true);
        insertHistoricalWeighing(farmId, animalId, new BigDecimal("500.00"), LocalDate.of(2026, 3, 10), worker.id());

        String accessToken = issueAccessToken(worker, OffsetDateTime.now(ZoneOffset.UTC));
        String body =
                """
                        {
                          "animalId": "%s",
                          "weightKg": 507.49,
                          "weighingDate": "2026-03-17",
                          "type": "routine"
                        }
                        """.formatted(animalId);

        String firstResponse = mockMvc.perform(post("/api/v1/weighings")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Idempotency-Key", "qa-006-weighing")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.animalId").value(animalId.toString()))
                .andExpect(jsonPath("$.weightKg").value(507.49))
                .andExpect(jsonPath("$.weighingDate").value("2026-03-17"))
                .andExpect(jsonPath("$.type").value("routine"))
                .andExpect(jsonPath("$.gmdKgPerDay").value(1.07))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondResponse = mockMvc.perform(post("/api/v1/weighings")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Idempotency-Key", "qa-006-weighing")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertEquals(firstResponse, secondResponse);
        assertEquals(
                2,
                jdbcTemplate.queryForObject("select count(*) from weighings where farm_id = ?", Integer.class, farmId)
        );
    }

    @Test
    void occurrenceLifecycleRemainsResolvableAndReopenableForOperationalStaff() throws Exception {
        UUID farmId = insertFarm("Fazenda Regressao Ocorrencia");
        UUID animalId = insertAnimal(farmId, null, "QA-006-O-001");
        UserAccount veterinarian = insertUser(farmId, "5511999999602", "Vet Ocorrencia", UserRole.VETERINARIAN, true);
        UUID occurrenceId = insertOccurrenceRecord(farmId, animalId, veterinarian.id(), "open", null);

        String accessToken = issueAccessToken(veterinarian, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/occurrences/{occurrenceId}/resolve", occurrenceId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"note\":\"Recovered after treatment.\"}"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.id").value(occurrenceId.toString()))
                .andExpect(jsonPath("$.status").value("resolved"))
                .andExpect(jsonPath("$.resolvedBy").value(veterinarian.id().toString()))
                .andExpect(jsonPath("$.resolvedAt", notNullValue()));

        assertEquals(
                "resolved",
                jdbcTemplate.queryForObject("select status from occurrences where id = ?", String.class, occurrenceId)
        );
        assertEquals(
                veterinarian.id().toString(),
                jdbcTemplate.queryForObject("select resolvido_por from occurrences where id = ?", String.class, occurrenceId)
        );

        mockMvc.perform(post("/api/v1/occurrences/{occurrenceId}/reopen", occurrenceId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.id").value(occurrenceId.toString()))
                .andExpect(jsonPath("$.status").value("open"));

        assertEquals(
                "open",
                jdbcTemplate.queryForObject("select status from occurrences where id = ?", String.class, occurrenceId)
        );
        assertNull(
                jdbcTemplate.queryForObject("select resolvido_por from occurrences where id = ?", Object.class, occurrenceId)
        );
    }

    @Test
    void medicationHistoryRetainsWithdrawalSemanticsAcrossCreateAndListFlows() throws Exception {
        UUID farmId = insertFarm("Fazenda Regressao Medicacao");
        UUID animalId = insertAnimal(farmId, null, "QA-006-M-001");
        UserAccount veterinarian = insertUser(farmId, "5511999999603", "Vet Medicacao", UserRole.VETERINARIAN, true);
        UUID occurrenceId = insertOccurrenceRecord(farmId, animalId, veterinarian.id(), "open", null);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        insertHistoricalMedication(
                farmId,
                animalId,
                null,
                veterinarian.id(),
                "Anti-inflamatorio",
                null,
                "Dor antiga",
                "5 ml",
                today.minusDays(90),
                3,
                new BigDecimal("12.50")
        );

        String accessToken = issueAccessToken(veterinarian, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/medications")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "animalId": "%s",
                                          "occurrenceId": "%s",
                                          "medicationName": "Ivermectin",
                                          "activeIngredient": "Ivermectina",
                                          "reason": "Ticks",
                                          "dosage": "10 ml",
                                          "withdrawalPeriodDays": 35,
                                          "applicationDate": "%s",
                                          "cost": "45.90"
                                        }
                                        """.formatted(animalId, occurrenceId, today.minusDays(2))
                        ))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.animalId").value(animalId.toString()))
                .andExpect(jsonPath("$.occurrenceId").value(occurrenceId.toString()))
                .andExpect(jsonPath("$.medicationName").value("Ivermectin"))
                .andExpect(jsonPath("$.withdrawalPeriodDays").value(35))
                .andExpect(jsonPath("$.applicationDate").value(today.minusDays(2).toString()))
                .andExpect(jsonPath("$.releaseDate").value(today.plusDays(33).toString()))
                .andExpect(jsonPath("$.inWithdrawalPeriod").value(true));

        mockMvc.perform(get("/api/v1/medications")
                        .param("animalId", animalId.toString())
                        .param("limit", "2")
                        .param("offset", "0")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.limit").value(2))
                .andExpect(jsonPath("$.offset").value(0))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].medicationName").value("Ivermectin"))
                .andExpect(jsonPath("$.items[0].releaseDate").value(today.plusDays(33).toString()))
                .andExpect(jsonPath("$.items[0].inWithdrawalPeriod").value(true))
                .andExpect(jsonPath("$.items[1].medicationName").value("Anti-inflamatorio"))
                .andExpect(jsonPath("$.items[1].inWithdrawalPeriod").value(false));
    }

    private void insertHistoricalWeighing(
            UUID farmId,
            UUID animalId,
            BigDecimal weightKg,
            LocalDate weighingDate,
            UUID recordedBy
    ) {
        UUID weighingId = UUID.randomUUID();
        OffsetDateTime now = nowUtc();

        jdbcTemplate.update(
                """
                        insert into weighings (
                            id, farm_id, animal_id, peso_kg, data_weighing, tipo, origem, registrado_por,
                            idempotency_key, audio_transcricao, created_at, updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                weighingId,
                farmId,
                animalId,
                weightKg,
                Date.valueOf(weighingDate),
                "rotina",
                "app",
                recordedBy,
                null,
                null,
                Timestamp.from(now.toInstant()),
                Timestamp.from(now.toInstant())
        );
    }

    private void insertHistoricalMedication(
            UUID farmId,
            UUID animalId,
            UUID occurrenceId,
            UUID recordedBy,
            String medicationName,
            String activeIngredient,
            String reason,
            String dosage,
            LocalDate applicationDate,
            int withdrawalPeriodDays,
            BigDecimal cost
    ) {
        UUID medicationId = UUID.randomUUID();
        OffsetDateTime now = nowUtc();

        jdbcTemplate.update(
                """
                        insert into medications (
                            id, farm_id, animal_id, occurrence_id, medication_name, principio_ativo, motivo,
                            dosagem, data_aplicacao, periodo_withdrawal, release_date, custo, origem,
                            registrado_por, created_at, updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                medicationId,
                farmId,
                animalId,
                occurrenceId,
                medicationName,
                activeIngredient,
                reason,
                dosage,
                Date.valueOf(applicationDate),
                withdrawalPeriodDays,
                Date.valueOf(applicationDate.plusDays(withdrawalPeriodDays)),
                cost,
                "app",
                recordedBy,
                Timestamp.from(now.toInstant()),
                Timestamp.from(now.toInstant())
        );
    }
}
