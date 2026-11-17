package br.com.curralzap.backend.animals.infrastructure;

import br.com.curralzap.backend.animals.application.AnimalDetailView;
import br.com.curralzap.backend.animals.application.AnimalListItemView;
import br.com.curralzap.backend.animals.application.AnimalListQuery;
import br.com.curralzap.backend.animals.application.AnimalListView;
import br.com.curralzap.backend.animals.application.AnimalLotView;
import br.com.curralzap.backend.animals.application.AnimalReadViewRepository;
import br.com.curralzap.backend.animals.application.AnimalSummaryView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class JdbcAnimalReadViewRepository implements AnimalReadViewRepository {

    private final JdbcTemplate jdbcTemplate;

    JdbcAnimalReadViewRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AnimalListView listFor(UUID farmId, AnimalListQuery query) {
        QueryParts queryParts = buildListQueryParts(farmId, query);

        Long total = jdbcTemplate.queryForObject(
                "select count(*) " + queryParts.fromAndWhereClause(),
                Long.class,
                queryParts.parameters().toArray()
        );

        List<Object> itemParameters = new ArrayList<>(queryParts.parameters());
        itemParameters.add(query.limit());
        itemParameters.add(query.offset());

        List<AnimalListItemView> items = jdbcTemplate.query(
                """
                        select
                            a.id,
                            a.identificacao,
                            a.nome,
                            a.raca,
                            a.sexo,
                            a.status,
                            a.lot_id,
                            l.nome as lot_name
                        """
                        + queryParts.fromAndWhereClause()
                        + " order by a.identificacao asc, a.id asc limit ? offset ?",
                (resultSet, rowNum) -> new AnimalListItemView(
                        UUID.fromString(resultSet.getString("id")),
                        resultSet.getString("identificacao"),
                        resultSet.getString("nome"),
                        toApiSex(resultSet.getString("sexo")),
                        resultSet.getString("raca"),
                        toApiStatus(resultSet.getString("status")),
                        resultSet.getString("lot_id") != null ? UUID.fromString(resultSet.getString("lot_id")) : null,
                        resultSet.getString("lot_name")
                ),
                itemParameters.toArray()
        );

        return new AnimalListView(
                items,
                total != null ? total : 0,
                query.limit(),
                query.offset()
        );
    }

    @Override
    public Optional<AnimalDetailView> findDetailById(UUID farmId, UUID animalId) {
        return jdbcTemplate.query(
                """
                        select
                            a.id,
                            a.identificacao,
                            a.nome,
                            a.raca,
                            a.sexo,
                            a.status,
                            a.data_nascimento,
                            a.data_entrada,
                            a.peso_nascimento,
                            a.dam_id,
                            a.sire_id,
                            a.farm_id,
                            a.lot_id,
                            l.nome as lot_name
                        from animals a
                        left join lots l on l.id = a.lot_id
                        where a.farm_id = ? and a.id = ?
                        """,
                (resultSet, rowNum) -> {
                    LocalDate today = LocalDate.now(ZoneOffset.UTC);
                    LocalDate birthDate = resultSet.getObject("data_nascimento", LocalDate.class);
                    LocalDate entryDate = resultSet.getObject("data_entrada", LocalDate.class);

                    return new AnimalDetailView(
                            UUID.fromString(resultSet.getString("id")),
                            resultSet.getString("identificacao"),
                            resultSet.getString("nome"),
                            toApiSex(resultSet.getString("sexo")),
                            resultSet.getString("raca"),
                            toApiStatus(resultSet.getString("status")),
                            birthDate,
                            entryDate,
                            UUID.fromString(resultSet.getString("farm_id")),
                            resultSet.getString("lot_id") != null
                                    ? new AnimalLotView(
                                    UUID.fromString(resultSet.getString("lot_id")),
                                    resultSet.getString("lot_name")
                            )
                                    : null,
                            new AnimalSummaryView(
                                    resultSet.getBigDecimal("peso_nascimento"),
                                    birthDate != null ? ChronoUnit.DAYS.between(birthDate, today) : null,
                                    entryDate != null ? ChronoUnit.DAYS.between(entryDate, today) : null
                            )
                    );
                },
                farmId,
                animalId
        ).stream().findFirst();
    }

    private QueryParts buildListQueryParts(UUID farmId, AnimalListQuery query) {
        StringBuilder sql = new StringBuilder(
                """
                        from animals a
                        left join lots l on l.id = a.lot_id
                        where a.farm_id = ?
                        """
        );
        List<Object> parameters = new ArrayList<>();
        parameters.add(farmId);

        if (query.search() != null) {
            sql.append(" and (lower(a.identificacao) like ? or lower(coalesce(a.nome, '')) like ?)");
            String searchLike = "%" + query.search().toLowerCase() + "%";
            parameters.add(searchLike);
            parameters.add(searchLike);
        }

        if (query.statusDatabaseValue() != null) {
            sql.append(" and a.status = ?");
            parameters.add(query.statusDatabaseValue());
        }

        if (query.lotId() != null) {
            sql.append(" and a.lot_id = ?");
            parameters.add(query.lotId());
        }

        if (query.sexDatabaseValue() != null) {
            sql.append(" and a.sexo = ?");
            parameters.add(query.sexDatabaseValue());
        }

        if (query.breed() != null) {
            sql.append(" and lower(coalesce(a.raca, '')) like ?");
            parameters.add("%" + query.breed().toLowerCase() + "%");
        }

        return new QueryParts(sql.toString(), parameters);
    }

    private static String toApiStatus(String databaseValue) {
        return switch (databaseValue) {
            case "ativo" -> "active";
            case "vendido" -> "sold";
            case "morto" -> "dead";
            case "transferido" -> "transferred";
            default -> throw new IllegalArgumentException("Unsupported animal status: " + databaseValue);
        };
    }

    private static String toApiSex(String databaseValue) {
        return switch (databaseValue) {
            case "macho" -> "male";
            case "femea" -> "female";
            default -> throw new IllegalArgumentException("Unsupported animal sex: " + databaseValue);
        };
    }

    private record QueryParts(
            String fromAndWhereClause,
            List<Object> parameters
    ) {
    }
}
