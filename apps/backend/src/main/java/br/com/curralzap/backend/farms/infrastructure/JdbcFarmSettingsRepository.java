package br.com.curralzap.backend.farms.infrastructure;

import br.com.curralzap.backend.farms.domain.FarmSettings;
import br.com.curralzap.backend.farms.domain.FarmSettingsRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
class JdbcFarmSettingsRepository implements FarmSettingsRepository {

    private final JdbcTemplate jdbcTemplate;

    JdbcFarmSettingsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<FarmSettings> findByFarmId(UUID farmId) {
        return jdbcTemplate.query(
                """
                        select
                            id,
                            nome,
                            rendimento_carcaca
                        from farms
                        where id = ?
                        """,
                (resultSet, rowNum) -> new FarmSettings(
                        UUID.fromString(resultSet.getString("id")),
                        resultSet.getString("nome"),
                        resultSet.getBigDecimal("rendimento_carcaca")
                ),
                farmId
        ).stream().findFirst();
    }

    @Override
    public FarmSettings save(FarmSettings settings) {
        jdbcTemplate.update(
                """
                        update farms
                        set
                            nome = ?,
                            rendimento_carcaca = ?,
                            updated_at = current_timestamp
                        where id = ?
                        """,
                settings.farmName(),
                settings.carcassYieldPercent(),
                settings.farmId()
        );

        return findByFarmId(settings.farmId()).orElseThrow();
    }
}
