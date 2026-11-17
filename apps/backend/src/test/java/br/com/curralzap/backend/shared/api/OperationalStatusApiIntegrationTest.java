package br.com.curralzap.backend.shared.api;

import br.com.curralzap.backend.shared.application.DependencyStatus;
import br.com.curralzap.backend.shared.application.OperationalDependencyProbe;
import br.com.curralzap.backend.support.BackendApiIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OperationalStatusApiIntegrationTest extends BackendApiIntegrationTestSupport {

    @MockitoBean(name = "databaseOperationalDependencyProbe")
    private OperationalDependencyProbe databaseProbe;

    @MockitoBean(name = "kafkaOperationalDependencyProbe")
    private OperationalDependencyProbe kafkaProbe;

    @Test
    void healthEndpointReturnsLivenessStatus() throws Exception {
        mockMvc.perform(get("/internal/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.runtimeRole").value("api"))
                .andExpect(jsonPath("$.checks[0].name").value("process"))
                .andExpect(jsonPath("$.checks[0].status").value("UP"));
    }

    @Test
    void readinessEndpointReturnsOkWhenDependenciesAreUp() throws Exception {
        given(databaseProbe.probe()).willReturn(new DependencyStatus("database", true, "Database query succeeded."));
        given(kafkaProbe.probe()).willReturn(new DependencyStatus("kafka", true, "Connected to Kafka cluster test-cluster."));

        mockMvc.perform(get("/internal/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.runtimeRole").value("api"))
                .andExpect(jsonPath("$.checks[0].name").value("database"))
                .andExpect(jsonPath("$.checks[0].status").value("UP"))
                .andExpect(jsonPath("$.checks[1].name").value("kafka"))
                .andExpect(jsonPath("$.checks[1].status").value("UP"));
    }

    @Test
    void readinessEndpointReturnsServiceUnavailableWhenAProbeFails() throws Exception {
        given(databaseProbe.probe()).willReturn(new DependencyStatus("database", true, "Database query succeeded."));
        given(kafkaProbe.probe()).willReturn(new DependencyStatus("kafka", false, "Kafka probe failed: TimeoutException: timed out"));

        mockMvc.perform(get("/internal/readiness"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.checks[0].status").value("UP"))
                .andExpect(jsonPath("$.checks[1].status").value("DOWN"))
                .andExpect(jsonPath("$.checks[1].detail").value("Kafka probe failed: TimeoutException: timed out"));
    }
}
