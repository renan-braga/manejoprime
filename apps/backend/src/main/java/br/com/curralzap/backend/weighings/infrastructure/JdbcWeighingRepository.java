package br.com.curralzap.backend.weighings.infrastructure;

import br.com.curralzap.backend.weighings.domain.Weighing;
import br.com.curralzap.backend.weighings.domain.WeighingRepository;
import br.com.curralzap.backend.weighings.domain.WeighingSource;
import br.com.curralzap.backend.weighings.domain.WeighingType;
import br.com.curralzap.backend.shared.domain.AppendFirstWriteGuard;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class JdbcWeighingRepository implements WeighingRepository {

    private final JdbcTemplate jdbcTemplate;

    JdbcWeighingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Weighing save(Weighing weighing) {
        AppendFirstWriteGuard.requireUnusedId("Weighing", weighing.id(), findById(weighing.id()).isPresent());

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        jdbcTemplate.update(
                """
                        insert into weighings (
                            id, farm_id, animal_id, peso_kg, data_weighing, tipo, origem, registrado_por,
                            idempotency_key, audio_transcricao, created_at, updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                weighing.id(),
                weighing.farmId(),
                weighing.animalId(),
                weighing.weightKg(),
                weighing.weighingDate(),
                weighing.type().databaseValue(),
                weighing.source().databaseValue(),
                weighing.recordedBy(),
                weighing.idempotencyKey(),
                null,
                Timestamp.from(now.toInstant()),
                Timestamp.from(now.toInstant())
        );

        return findById(weighing.id()).orElseThrow();
    }

    @Override
    public Optional<Weighing> findByFarmIdAndIdempotencyKey(UUID farmId, String idempotencyKey) {
        return jdbcTemplate.query(
                """
                        select
                            id,
                            farm_id,
                            animal_id,
                            peso_kg,
                            data_weighing,
                            tipo,
                            origem,
                            registrado_por,
                            idempotency_key,
                            created_at,
                            updated_at
                        from weighings
                        where farm_id = ? and idempotency_key = ?
                        """,
                (resultSet, rowNum) -> mapWeighing(resultSet),
                farmId,
                idempotencyKey
        ).stream().findFirst();
    }

    @Override
    public Optional<Weighing> findLatestBefore(UUID farmId, UUID animalId, LocalDate weighingDate) {
        return jdbcTemplate.query(
                """
                        select
                            id,
                            farm_id,
                            animal_id,
                            peso_kg,
                            data_weighing,
                            tipo,
                            origem,
                            registrado_por,
                            idempotency_key,
                            created_at,
                            updated_at
                        from weighings
                        where farm_id = ? and animal_id = ? and data_weighing < ?
                        order by data_weighing desc, created_at desc
                        limit 1
                        """,
                (resultSet, rowNum) -> mapWeighing(resultSet),
                farmId,
                animalId,
                weighingDate
        ).stream().findFirst();
    }

    @Override
    public List<Weighing> findHistory(UUID farmId, UUID animalId) {
        return jdbcTemplate.query(
                """
                        select
                            id,
                            farm_id,
                            animal_id,
                            peso_kg,
                            data_weighing,
                            tipo,
                            origem,
                            registrado_por,
                            idempotency_key,
                            created_at,
                            updated_at
                        from weighings
                        where farm_id = ? and animal_id = ?
                        order by data_weighing desc, created_at desc
                        """,
                (resultSet, rowNum) -> mapWeighing(resultSet),
                farmId,
                animalId
        );
    }

    private Optional<Weighing> findById(UUID weighingId) {
        return jdbcTemplate.query(
                """
                        select
                            id,
                            farm_id,
                            animal_id,
                            peso_kg,
                            data_weighing,
                            tipo,
                            origem,
                            registrado_por,
                            idempotency_key,
                            created_at,
                            updated_at
                        from weighings
                        where id = ?
                        """,
                (resultSet, rowNum) -> mapWeighing(resultSet),
                weighingId
        ).stream().findFirst();
    }

    private Weighing mapWeighing(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        return new Weighing(
                UUID.fromString(resultSet.getString("id")),
                UUID.fromString(resultSet.getString("farm_id")),
                UUID.fromString(resultSet.getString("animal_id")),
                resultSet.getBigDecimal("peso_kg"),
                resultSet.getDate("data_weighing").toLocalDate(),
                WeighingType.fromDatabaseValue(resultSet.getString("tipo")),
                WeighingSource.fromDatabaseValue(resultSet.getString("origem")),
                UUID.fromString(resultSet.getString("registrado_por")),
                resultSet.getString("idempotency_key"),
                resultSet.getObject("created_at", OffsetDateTime.class),
                resultSet.getObject("updated_at", OffsetDateTime.class)
        );
    }
}
