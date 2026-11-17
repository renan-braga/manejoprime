package br.com.curralzap.backend.occurrences.infrastructure;

import br.com.curralzap.backend.occurrences.application.OccurrenceListItemView;
import br.com.curralzap.backend.occurrences.application.OccurrenceListQuery;
import br.com.curralzap.backend.occurrences.application.OccurrenceListView;
import br.com.curralzap.backend.occurrences.application.OccurrenceReadViewRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
class JdbcOccurrenceReadViewRepository implements OccurrenceReadViewRepository {

    private final JdbcTemplate jdbcTemplate;

    JdbcOccurrenceReadViewRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public OccurrenceListView listFor(UUID farmId, OccurrenceListQuery query) {
        QueryParts queryParts = buildQueryParts(farmId, query);

        Long total = jdbcTemplate.queryForObject(
                "select count(*) from occurrences o join animals a on a.farm_id = o.farm_id and a.id = o.animal_id "
                        + queryParts.whereClause(),
                Long.class,
                queryParts.parameters().toArray()
        );

        List<Object> itemParameters = new ArrayList<>(queryParts.parameters());
        itemParameters.add(query.limit());
        itemParameters.add(query.offset());

        List<OccurrenceListItemView> items = jdbcTemplate.query(
                """
                        select
                            o.id,
                            o.animal_id,
                            a.identificacao as animal_identifier,
                            a.nome as animal_name,
                            o.tipo,
                            o.gravidade,
                            o.status,
                            o.descricao,
                            o.foto_url,
                            o.origem,
                            o.registrado_por,
                            o.resolvido_por,
                            o.created_at,
                            o.updated_at
                        from occurrences o
                        join animals a on a.farm_id = o.farm_id and a.id = o.animal_id
                        """
                        + queryParts.whereClause()
                        + """
                         order by
                            case when o.status = 'resolved' then 1 else 0 end,
                            o.updated_at desc,
                            o.id desc
                         limit ? offset ?
                        """,
                (resultSet, rowNum) -> new OccurrenceListItemView(
                        UUID.fromString(resultSet.getString("id")),
                        UUID.fromString(resultSet.getString("animal_id")),
                        resultSet.getString("animal_identifier"),
                        resultSet.getString("animal_name"),
                        resultSet.getString("tipo"),
                        resultSet.getString("gravidade"),
                        resultSet.getString("status"),
                        resultSet.getString("descricao"),
                        resultSet.getString("foto_url"),
                        resultSet.getString("origem"),
                        UUID.fromString(resultSet.getString("registrado_por")),
                        resultSet.getObject("resolvido_por") == null
                                ? null
                                : UUID.fromString(resultSet.getString("resolvido_por")),
                        resultSet.getObject("created_at", java.time.OffsetDateTime.class),
                        resultSet.getObject("updated_at", java.time.OffsetDateTime.class)
                ),
                itemParameters.toArray()
        );

        return new OccurrenceListView(
                items,
                total != null ? total : 0,
                query.limit(),
                query.offset()
        );
    }

    private QueryParts buildQueryParts(UUID farmId, OccurrenceListQuery query) {
        StringBuilder whereClause = new StringBuilder(" where o.farm_id = ?");
        List<Object> parameters = new ArrayList<>();
        parameters.add(farmId);

        if (query.animalId() != null) {
            whereClause.append(" and o.animal_id = ?");
            parameters.add(query.animalId());
        }

        if (query.status() != null) {
            whereClause.append(" and o.status = ?");
            parameters.add(query.status());
        }

        if (query.severity() != null) {
            whereClause.append(" and o.gravidade = ?");
            parameters.add(query.severity());
        }

        return new QueryParts(whereClause.toString(), parameters);
    }

    private record QueryParts(
            String whereClause,
            List<Object> parameters
    ) {
    }
}
