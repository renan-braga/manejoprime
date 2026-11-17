package br.com.curralzap.backend.occurrences.infrastructure;

import br.com.curralzap.backend.occurrences.domain.Occurrence;
import br.com.curralzap.backend.occurrences.domain.OccurrenceRepository;
import br.com.curralzap.backend.occurrences.domain.OccurrenceSeverity;
import br.com.curralzap.backend.occurrences.domain.OccurrenceSource;
import br.com.curralzap.backend.occurrences.domain.OccurrenceStatus;
import br.com.curralzap.backend.occurrences.domain.OccurrenceType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
class JdbcOccurrenceRepository implements OccurrenceRepository {

    private final JdbcTemplate jdbcTemplate;

    JdbcOccurrenceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Occurrence> findByIdAndFarmId(UUID occurrenceId, UUID farmId) {
        return jdbcTemplate.query(
                """
                        select
                            id,
                            farm_id,
                            animal_id,
                            tipo,
                            gravidade,
                            descricao,
                            foto_url,
                            origem,
                            registrado_por,
                            status,
                            resolvido_por,
                            created_at,
                            updated_at
                        from occurrences
                        where id = ? and farm_id = ?
                        """,
                (resultSet, rowNum) -> mapOccurrence(resultSet),
                occurrenceId,
                farmId
        ).stream().findFirst();
    }

    @Override
    public Occurrence insert(Occurrence occurrence) {
        jdbcTemplate.update(
                """
                        insert into occurrences
                            (id, farm_id, animal_id, tipo, gravidade, descricao, foto_url, origem, registrado_por, status, resolvido_por, created_at, updated_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                occurrence.id(),
                occurrence.farmId(),
                occurrence.animalId(),
                occurrence.type().databaseValue(),
                occurrence.severity().databaseValue(),
                occurrence.description(),
                occurrence.photoUrl(),
                occurrence.source().databaseValue(),
                occurrence.recordedBy(),
                occurrence.status().databaseValue(),
                occurrence.resolvedBy(),
                occurrence.createdAt(),
                occurrence.updatedAt()
        );
        return findByIdAndFarmId(occurrence.id(), occurrence.farmId()).orElseThrow();
    }

    @Override
    public void delete(UUID occurrenceId) {
        jdbcTemplate.update("delete from occurrences where id = ?", occurrenceId);
    }

    @Override
    public Occurrence save(Occurrence occurrence) {
        jdbcTemplate.update(
                """
                        update occurrences
                        set status = ?, resolvido_por = ?, updated_at = ?
                        where id = ? and farm_id = ?
                        """,
                occurrence.status().databaseValue(),
                occurrence.resolvedBy(),
                occurrence.updatedAt(),
                occurrence.id(),
                occurrence.farmId()
        );
        return findByIdAndFarmId(occurrence.id(), occurrence.farmId()).orElseThrow();
    }

    private Occurrence mapOccurrence(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        return new Occurrence(
                UUID.fromString(resultSet.getString("id")),
                UUID.fromString(resultSet.getString("farm_id")),
                UUID.fromString(resultSet.getString("animal_id")),
                OccurrenceType.fromDatabaseValue(resultSet.getString("tipo")),
                OccurrenceSeverity.fromDatabaseValue(resultSet.getString("gravidade")),
                resultSet.getString("descricao"),
                resultSet.getString("foto_url"),
                OccurrenceSource.fromDatabaseValue(resultSet.getString("origem")),
                UUID.fromString(resultSet.getString("registrado_por")),
                OccurrenceStatus.fromDatabaseValue(resultSet.getString("status")),
                resultSet.getObject("resolvido_por") == null ? null : UUID.fromString(resultSet.getString("resolvido_por")),
                resultSet.getObject("created_at", OffsetDateTime.class),
                resultSet.getObject("updated_at", OffsetDateTime.class)
        );
    }
}
