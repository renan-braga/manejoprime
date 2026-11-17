package br.com.curralzap.backend.lots.infrastructure;

import br.com.curralzap.backend.lots.application.LotDetailView;
import br.com.curralzap.backend.lots.application.LotListItemView;
import br.com.curralzap.backend.lots.application.LotListQuery;
import br.com.curralzap.backend.lots.application.LotListView;
import br.com.curralzap.backend.lots.application.LotReadViewRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class JdbcLotReadViewRepository implements LotReadViewRepository {

    private final JdbcTemplate jdbcTemplate;

    JdbcLotReadViewRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public LotListView listFor(UUID farmId, LotListQuery query) {
        QueryParts queryParts = buildQueryParts(farmId, query);

        Long total = jdbcTemplate.queryForObject(
                "select count(*) from lots l " + queryParts.whereClause(),
                Long.class,
                queryParts.parameters().toArray()
        );

        List<Object> itemParameters = new ArrayList<>(queryParts.parameters());
        itemParameters.add(query.limit());
        itemParameters.add(query.offset());

        List<LotListItemView> items = jdbcTemplate.query(
                """
                        select
                            l.id,
                            l.nome,
                            l.finalidade,
                            l.pasto_area,
                            count(a.id) as animals_count
                        from lots l
                        left join animals a on a.lot_id = l.id and a.farm_id = l.farm_id
                        """
                        + queryParts.whereClause()
                        + " group by l.id, l.nome, l.finalidade, l.pasto_area"
                        + " order by l.nome asc, l.id asc limit ? offset ?",
                (resultSet, rowNum) -> new LotListItemView(
                        UUID.fromString(resultSet.getString("id")),
                        resultSet.getString("nome"),
                        resultSet.getString("finalidade"),
                        resultSet.getString("pasto_area"),
                        resultSet.getLong("animals_count")
                ),
                itemParameters.toArray()
        );

        return new LotListView(
                items,
                total != null ? total : 0,
                query.limit(),
                query.offset()
        );
    }

    @Override
    public Optional<LotDetailView> findDetailById(UUID farmId, UUID lotId) {
        return jdbcTemplate.query(
                """
                        select
                            l.id,
                            l.farm_id,
                            l.nome,
                            l.finalidade,
                            l.pasto_area,
                            count(a.id) as animals_count
                        from lots l
                        left join animals a on a.lot_id = l.id and a.farm_id = l.farm_id
                        where l.farm_id = ? and l.id = ?
                        group by l.id, l.farm_id, l.nome, l.finalidade, l.pasto_area
                        """,
                (resultSet, rowNum) -> new LotDetailView(
                        UUID.fromString(resultSet.getString("id")),
                        UUID.fromString(resultSet.getString("farm_id")),
                        resultSet.getString("nome"),
                        resultSet.getString("finalidade"),
                        resultSet.getString("pasto_area"),
                        resultSet.getLong("animals_count")
                ),
                farmId,
                lotId
        ).stream().findFirst();
    }

    private QueryParts buildQueryParts(UUID farmId, LotListQuery query) {
        StringBuilder whereClause = new StringBuilder(
                """
                        where l.farm_id = ?
                        """
        );
        List<Object> parameters = new ArrayList<>();
        parameters.add(farmId);

        if (query.search() != null) {
            whereClause.append(" and lower(l.nome) like ?");
            parameters.add("%" + query.search().toLowerCase() + "%");
        }

        if (query.purposeDatabaseValue() != null) {
            whereClause.append(" and l.finalidade = ?");
            parameters.add(query.purposeDatabaseValue());
        }

        return new QueryParts(
                whereClause.toString(),
                parameters
        );
    }

    private record QueryParts(
            String whereClause,
            List<Object> parameters
    ) {
    }
}
