package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.WhatsappSupportMessageListItemView;
import br.com.curralzap.backend.whatsapp.application.WhatsappSupportMessageListView;
import br.com.curralzap.backend.whatsapp.application.WhatsappSupportMessageQuery;
import br.com.curralzap.backend.whatsapp.application.WhatsappSupportReadViewRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
class JdbcWhatsappSupportReadViewRepository implements WhatsappSupportReadViewRepository {

    private final JdbcTemplate jdbcTemplate;

    JdbcWhatsappSupportReadViewRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public WhatsappSupportMessageListView listFor(UUID farmId, UUID userId, WhatsappSupportMessageQuery query) {
        QueryParts queryParts = buildQueryParts(farmId, userId, query);

        Long total = jdbcTemplate.queryForObject(
                "select count(*) from whatsapp_inbound_messages m " + queryParts.whereClause(),
                Long.class,
                queryParts.parameters().toArray()
        );

        List<Object> itemParameters = new ArrayList<>(queryParts.parameters());
        itemParameters.add(query.limit());
        itemParameters.add(query.offset());

        List<WhatsappSupportMessageListItemView> items = jdbcTemplate.query(
                """
                        select
                            m.id,
                            m.provider_message_id,
                            m.phone_number,
                            m.correlation_id,
                            m.message_type,
                            m.status,
                            m.failure_reason,
                            m.received_at,
                            m.updated_at
                        from whatsapp_inbound_messages m
                        """
                        + queryParts.whereClause()
                        + " order by m.created_at desc, m.id desc limit ? offset ?",
                (resultSet, rowNum) -> new WhatsappSupportMessageListItemView(
                        UUID.fromString(resultSet.getString("id")),
                        resultSet.getString("provider_message_id"),
                        resultSet.getString("phone_number"),
                        resultSet.getString("correlation_id"),
                        resultSet.getString("message_type"),
                        resultSet.getString("status").toLowerCase(),
                        resultSet.getString("failure_reason"),
                        resultSet.getObject("received_at", java.time.OffsetDateTime.class),
                        resultSet.getObject("updated_at", java.time.OffsetDateTime.class)
                ),
                itemParameters.toArray()
        );

        return new WhatsappSupportMessageListView(
                items,
                total != null ? total : 0,
                query.limit(),
                query.offset()
        );
    }

    private QueryParts buildQueryParts(UUID farmId, UUID userId, WhatsappSupportMessageQuery query) {
        // Include farm_id IS NULL messages (PENDING_FARM_SELECTION / FARM_SELECTED) for the current user
        StringBuilder whereClause = new StringBuilder(
                """
                        where (m.farm_id = ? or (m.farm_id is null and m.user_id = ?))
                        """
        );
        List<Object> parameters = new ArrayList<>();
        parameters.add(farmId);
        parameters.add(userId);

        if (query.search() != null) {
            whereClause.append(" and (lower(m.provider_message_id) like ? or lower(coalesce(m.correlation_id, '')) like ? or m.phone_number like ?)");
            String searchLike = "%" + query.search().toLowerCase() + "%";
            parameters.add(searchLike);
            parameters.add(searchLike);
            parameters.add("%" + query.search().replaceFirst("^\\+", "") + "%");
        }

        if (query.status() != null) {
            whereClause.append(" and m.status = ?");
            parameters.add(query.status().name());
        }

        return new QueryParts(whereClause.toString(), parameters);
    }

    private record QueryParts(
            String whereClause,
            List<Object> parameters
    ) {
    }
}
