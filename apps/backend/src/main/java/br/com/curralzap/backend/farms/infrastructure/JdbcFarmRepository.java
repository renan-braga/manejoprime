package br.com.curralzap.backend.farms.infrastructure;

import br.com.curralzap.backend.farms.domain.Farm;
import br.com.curralzap.backend.farms.domain.FarmRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Repository
class JdbcFarmRepository implements FarmRepository {

    private final JdbcTemplate jdbcTemplate;

    JdbcFarmRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Farm save(Farm farm) {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        jdbcTemplate.update(
                """
                        insert into farms (id, nome, rendimento_carcaca, created_at, updated_at)
                        values (?, ?, 50.00, ?, ?)
                        """,
                id,
                farm.name(),
                Timestamp.from(now.toInstant()),
                Timestamp.from(now.toInstant())
        );

        return new Farm(id, farm.name(), null, now, now);
    }

    @Override
    public void setOwner(UUID farmId, UUID ownerUserId) {
        jdbcTemplate.update(
                "update farms set owner_user_id = ? where id = ?",
                ownerUserId, farmId
        );
    }

    @Override
    public int countOwnedByUserId(UUID ownerUserId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from farms where owner_user_id = ?",
                Integer.class, ownerUserId);
        return count != null ? count : 0;
    }

    @Override
    public java.util.Optional<UUID> findFirstOwnedByUserId(UUID ownerUserId) {
        return jdbcTemplate.query(
                "select id from farms where owner_user_id = ? order by created_at asc limit 1",
                (rs, n) -> UUID.fromString(rs.getString("id")),
                ownerUserId
        ).stream().findFirst();
    }
}
