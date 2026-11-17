package br.com.curralzap.backend.users.infrastructure;

import br.com.curralzap.backend.users.application.UserDetailView;
import br.com.curralzap.backend.users.application.UserListItemView;
import br.com.curralzap.backend.users.application.UserListQuery;
import br.com.curralzap.backend.users.application.UserListView;
import br.com.curralzap.backend.users.application.UserReadViewRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class JdbcUserReadViewRepository implements UserReadViewRepository {

    private final JdbcTemplate jdbcTemplate;

    JdbcUserReadViewRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public UserListView listFor(UUID farmId, UserListQuery query) {
        QueryParts queryParts = buildQueryParts(farmId, query);

        Long total = jdbcTemplate.queryForObject(
                "select count(*) from users u " + queryParts.whereClause(),
                Long.class,
                queryParts.parameters().toArray()
        );

        List<Object> itemParameters = new ArrayList<>(queryParts.parameters());
        itemParameters.add(query.limit());
        itemParameters.add(query.offset());

        List<UserListItemView> items = jdbcTemplate.query(
                """
                        select
                            u.id,
                            u.nome,
                            u.telefone,
                            u.role,
                            u.ativo
                        from users u
                        """
                        + queryParts.whereClause()
                        + " order by u.nome asc, u.id asc limit ? offset ?",
                (resultSet, rowNum) -> new UserListItemView(
                        UUID.fromString(resultSet.getString("id")),
                        resultSet.getString("nome"),
                        resultSet.getString("telefone"),
                        resultSet.getString("role"),
                        resultSet.getBoolean("ativo")
                ),
                itemParameters.toArray()
        );

        return new UserListView(
                items,
                total != null ? total : 0,
                query.limit(),
                query.offset()
        );
    }

    @Override
    public Optional<UserDetailView> findDetailById(UUID farmId, UUID userId) {
        return jdbcTemplate.query(
                """
                        select
                            u.id,
                            u.farm_id,
                            u.nome,
                            u.telefone,
                            u.role,
                            u.ativo
                        from users u
                        where u.farm_id = ? and u.id = ?
                        """,
                (resultSet, rowNum) -> new UserDetailView(
                        UUID.fromString(resultSet.getString("id")),
                        UUID.fromString(resultSet.getString("farm_id")),
                        resultSet.getString("nome"),
                        resultSet.getString("telefone"),
                        resultSet.getString("role"),
                        resultSet.getBoolean("ativo")
                ),
                farmId,
                userId
        ).stream().findFirst();
    }

    private QueryParts buildQueryParts(UUID farmId, UserListQuery query) {
        StringBuilder whereClause = new StringBuilder(
                """
                        where u.farm_id = ?
                        """
        );
        List<Object> parameters = new ArrayList<>();
        parameters.add(farmId);

        if (query.search() != null) {
            whereClause.append(" and (lower(u.nome) like ? or u.telefone like ?)");
            String searchLike = "%" + query.search().toLowerCase() + "%";
            parameters.add(searchLike);
            parameters.add("%" + query.search() + "%");
        }

        if (query.roleDatabaseValue() != null) {
            whereClause.append(" and u.role = ?");
            parameters.add(query.roleDatabaseValue());
        }

        if (query.active() != null) {
            whereClause.append(" and u.ativo = ?");
            parameters.add(query.active());
        }

        return new QueryParts(whereClause.toString(), parameters);
    }

    private record QueryParts(
            String whereClause,
            List<Object> parameters
    ) {
    }
}
