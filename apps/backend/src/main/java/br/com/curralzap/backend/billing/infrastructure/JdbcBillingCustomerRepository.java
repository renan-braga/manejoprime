package br.com.curralzap.backend.billing.infrastructure;

import br.com.curralzap.backend.billing.application.BillingCustomerData;
import br.com.curralzap.backend.billing.application.BillingCustomerRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@Repository
class JdbcBillingCustomerRepository implements BillingCustomerRepository {

    private final JdbcTemplate jdbcTemplate;

    JdbcBillingCustomerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<BillingCustomerData> findByFarmId(UUID farmId) {
        return jdbcTemplate.query(
                "select farm_id, customer_type, name, document, email, phone," +
                "       postal_code, address, address_number, city, state" +
                " from billing_customer_data where farm_id = ?",
                (rs, n) -> map(rs),
                farmId
        ).stream().findFirst();
    }

    @Override
    public void upsert(BillingCustomerData data) {
        jdbcTemplate.update(
                "insert into billing_customer_data" +
                " (farm_id, customer_type, name, document, email, phone," +
                "  postal_code, address, address_number, city, state, updated_at)" +
                " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" +
                " on conflict (farm_id) do update set" +
                "   customer_type  = excluded.customer_type," +
                "   name           = excluded.name," +
                "   document       = excluded.document," +
                "   email          = excluded.email," +
                "   phone          = excluded.phone," +
                "   postal_code    = excluded.postal_code," +
                "   address        = excluded.address," +
                "   address_number = excluded.address_number," +
                "   city           = excluded.city," +
                "   state          = excluded.state," +
                "   updated_at     = excluded.updated_at",
                data.farmId(),
                data.customerType(),
                data.name(),
                data.document(),
                data.email(),
                data.phone(),
                data.postalCode(),
                data.address(),
                data.addressNumber(),
                data.city(),
                data.state(),
                OffsetDateTime.now(ZoneOffset.UTC)
        );
    }

    private static BillingCustomerData map(ResultSet rs) throws SQLException {
        return new BillingCustomerData(
                UUID.fromString(rs.getString("farm_id")),
                rs.getString("customer_type"),
                rs.getString("name"),
                rs.getString("document"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("postal_code"),
                rs.getString("address"),
                rs.getString("address_number"),
                rs.getString("city"),
                rs.getString("state")
        );
    }
}
