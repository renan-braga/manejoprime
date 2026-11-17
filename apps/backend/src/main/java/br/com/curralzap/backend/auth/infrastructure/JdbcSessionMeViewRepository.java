package br.com.curralzap.backend.auth.infrastructure;

import br.com.curralzap.backend.auth.application.RolePermissionSummary;
import br.com.curralzap.backend.auth.application.SessionMeView;
import br.com.curralzap.backend.auth.application.SessionMeViewRepository;
import br.com.curralzap.backend.users.domain.UserRole;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
class JdbcSessionMeViewRepository implements SessionMeViewRepository {

    private final JdbcTemplate jdbcTemplate;

    JdbcSessionMeViewRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<SessionMeView> findByUserIdAndFarmId(UUID userId, UUID farmId) {
        return jdbcTemplate.query(
                """
                        select u.id, u.telefone, u.email, u.nome, u.role, f.id as farm_id, f.nome as farm_name,
                               (select count(*) from user_farm_memberships m where m.user_id = u.id) as farm_count,
                               (select count(*) from animals a where a.farm_id = f.id) as animal_count,
                               (select count(*) from user_farm_memberships m2 where m2.farm_id = f.id) as user_count
                        from users u
                        join farms f on f.id = ?
                        where u.id = ?
                        """,
                (resultSet, rowNum) -> {
                    UserRole role = UserRole.fromDatabaseValue(resultSet.getString("role"));
                    return new SessionMeView(
                            UUID.fromString(resultSet.getString("id")),
                            resultSet.getString("nome"),
                            resultSet.getString("telefone"),
                            resultSet.getString("email"),
                            resultSet.getString("role"),
                            UUID.fromString(resultSet.getString("farm_id")),
                            resultSet.getString("farm_name"),
                            RolePermissionSummary.permissionsFor(role),
                            null,
                            resultSet.getInt("farm_count"),
                            resultSet.getLong("animal_count"),
                            resultSet.getInt("user_count")
                    );
                },
                farmId, userId
        ).stream().findFirst();
    }
}
