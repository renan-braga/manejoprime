package br.com.curralzap.backend.shared.infrastructure;

import br.com.curralzap.backend.shared.application.DependencyStatus;
import br.com.curralzap.backend.shared.application.OperationalDependencyProbe;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
class KafkaOperationalDependencyProbe implements OperationalDependencyProbe {

    private final String bootstrapServers;
    private final int timeoutMillis;

    KafkaOperationalDependencyProbe(
            @Value("${spring.kafka.bootstrap-servers:}") String bootstrapServers,
            @Value("${curralzap.ops.kafka.readiness-timeout-ms:3000}") int timeoutMillis
    ) {
        this.bootstrapServers = bootstrapServers;
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public DependencyStatus probe() {
        if (bootstrapServers == null || bootstrapServers.isBlank()) {
            return new DependencyStatus("kafka", false, "Kafka bootstrap servers are not configured.");
        }

        try (AdminClient adminClient = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, timeoutMillis,
                AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, timeoutMillis
        ))) {
            String clusterId = adminClient.describeCluster().clusterId().get(timeoutMillis, TimeUnit.MILLISECONDS);
            if (clusterId == null || clusterId.isBlank()) {
                return new DependencyStatus("kafka", true, "Kafka responded without a cluster id.");
            }
            return new DependencyStatus("kafka", true, "Connected to Kafka cluster " + clusterId + ".");
        } catch (Exception exception) {
            return new DependencyStatus("kafka", false, formatFailure(exception));
        }
    }

    private String formatFailure(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "Kafka probe failed: " + exception.getClass().getSimpleName();
        }
        return "Kafka probe failed: " + exception.getClass().getSimpleName() + ": " + message;
    }
}
