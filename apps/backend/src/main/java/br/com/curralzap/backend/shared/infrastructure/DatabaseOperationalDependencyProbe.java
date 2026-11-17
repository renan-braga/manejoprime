package br.com.curralzap.backend.shared.infrastructure;

import br.com.curralzap.backend.shared.application.DependencyStatus;
import br.com.curralzap.backend.shared.application.OperationalDependencyProbe;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class DatabaseOperationalDependencyProbe implements OperationalDependencyProbe {

    private final JdbcTemplate jdbcTemplate;

    DatabaseOperationalDependencyProbe(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DependencyStatus probe() {
        try {
            Integer value = jdbcTemplate.queryForObject("select 1", Integer.class);
            if (Integer.valueOf(1).equals(value)) {
                return new DependencyStatus("database", true, "Database query succeeded.");
            }
            return new DependencyStatus("database", false, "Database probe returned an unexpected value.");
        } catch (Exception exception) {
            return new DependencyStatus("database", false, formatFailure(exception));
        }
    }

    private String formatFailure(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "Database probe failed: " + exception.getClass().getSimpleName();
        }
        return "Database probe failed: " + exception.getClass().getSimpleName() + ": " + message;
    }
}
