package br.com.curralzap.backend.offers.infrastructure;

import br.com.curralzap.backend.offers.application.OfferDetailView;
import br.com.curralzap.backend.offers.application.OfferListItemView;
import br.com.curralzap.backend.offers.application.OfferReadViewRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class JdbcOfferReadViewRepository implements OfferReadViewRepository {

    private final JdbcTemplate jdbcTemplate;

    JdbcOfferReadViewRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<OfferListItemView> listByFarmId(UUID farmId, String statusFilter) {
        List<Object> params = new ArrayList<>();
        params.add(farmId);

        String whereClause = "where o.farm_id = ?";
        if (statusFilter != null && !statusFilter.isBlank()) {
            whereClause += " and o.status = ?";
            params.add(statusFilter);
        }

        return jdbcTemplate.query(
                """
                        select
                            o.id, o.farm_id, o.title, o.asking_price, o.buyer_contact,
                            o.expires_at, o.status, o.created_at, o.updated_at,
                            count(oa.animal_id) as animal_count
                        from offers o
                        left join offer_animals oa on oa.offer_id = o.id
                        """
                        + whereClause
                        + " group by o.id, o.farm_id, o.title, o.asking_price, o.buyer_contact,"
                        + " o.expires_at, o.status, o.created_at, o.updated_at"
                        + " order by o.created_at desc",
                (rs, rowNum) -> new OfferListItemView(
                        UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("farm_id")),
                        rs.getString("title"),
                        rs.getBigDecimal("asking_price"),
                        rs.getString("buyer_contact"),
                        rs.getDate("expires_at") != null ? rs.getDate("expires_at").toLocalDate() : null,
                        rs.getString("status"),
                        rs.getInt("animal_count"),
                        rs.getTimestamp("created_at").toInstant().atOffset(java.time.ZoneOffset.UTC),
                        rs.getTimestamp("updated_at").toInstant().atOffset(java.time.ZoneOffset.UTC)
                ),
                params.toArray()
        );
    }

    @Override
    public Optional<OfferDetailView> findDetailByIdAndFarmId(UUID offerId, UUID farmId) {
        return jdbcTemplate.query(
                """
                        select
                            id, farm_id, title, notes, asking_price,
                            buyer_contact, expires_at, status, created_at, updated_at
                        from offers
                        where id = ? and farm_id = ?
                        """,
                (rs, rowNum) -> {
                    UUID id = UUID.fromString(rs.getString("id"));
                    List<UUID> animalIds = jdbcTemplate.query(
                            "select animal_id from offer_animals where offer_id = ?",
                            (aRs, aRowNum) -> UUID.fromString(aRs.getString("animal_id")),
                            id
                    );
                    return new OfferDetailView(
                            id,
                            UUID.fromString(rs.getString("farm_id")),
                            rs.getString("title"),
                            rs.getString("notes"),
                            rs.getBigDecimal("asking_price"),
                            rs.getString("buyer_contact"),
                            rs.getDate("expires_at") != null ? rs.getDate("expires_at").toLocalDate() : null,
                            rs.getString("status"),
                            animalIds,
                            rs.getTimestamp("created_at").toInstant()
                                    .atOffset(java.time.ZoneOffset.UTC),
                            rs.getTimestamp("updated_at").toInstant()
                                    .atOffset(java.time.ZoneOffset.UTC)
                    );
                },
                offerId,
                farmId
        ).stream().findFirst();
    }
}
