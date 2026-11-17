package br.com.curralzap.backend.farms.infrastructure;

import br.com.curralzap.backend.farms.domain.FarmMembershipDetails;
import br.com.curralzap.backend.farms.domain.UserFarmMembershipRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
class JdbcUserFarmMembershipRepository implements UserFarmMembershipRepository {

    private final JdbcTemplate jdbcTemplate;

    JdbcUserFarmMembershipRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(UUID userId, UUID farmId, String role) {
        int updated = jdbcTemplate.update(
                "update user_farm_memberships set role = ? where user_id = ? and farm_id = ?",
                role, userId, farmId
        );
        if (updated == 0) {
            jdbcTemplate.update(
                    "insert into user_farm_memberships (user_id, farm_id, role) values (?, ?, ?)",
                    userId, farmId, role
            );
        }
    }

    @Override
    public int countByUserId(UUID userId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from user_farm_memberships where user_id = ?",
                Integer.class, userId
        );
        return count != null ? count : 0;
    }

    @Override
    public boolean existsByUserIdAndFarmId(UUID userId, UUID farmId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from user_farm_memberships where user_id = ? and farm_id = ?",
                Integer.class, userId, farmId
        );
        return count != null && count > 0;
    }

    @Override
    public List<FarmMembershipDetails> findWithFarmNameByUserId(UUID userId) {
        return jdbcTemplate.query(
                "select m.farm_id, f.nome as farm_name, m.role" +
                " from user_farm_memberships m" +
                " join farms f on f.id = m.farm_id" +
                " where m.user_id = ?" +
                " order by f.created_at asc",
                (rs, n) -> new FarmMembershipDetails(
                        UUID.fromString(rs.getString("farm_id")),
                        rs.getString("farm_name"),
                        rs.getString("role")
                ),
                userId
        );
    }
}
