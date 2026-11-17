package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.domain.WhatsappFarmSession;
import br.com.curralzap.backend.whatsapp.domain.WhatsappFarmSessionRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class JdbcWhatsappFarmSessionRepository implements WhatsappFarmSessionRepository {

    private final JdbcTemplate jdbcTemplate;

    JdbcWhatsappFarmSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<WhatsappFarmSession> findByPhoneAndDate(String phoneNumber, LocalDate date) {
        List<WhatsappFarmSession> results = jdbcTemplate.query(
                "select phone_number, farm_id, farm_name, selected_date" +
                " from whatsapp_farm_sessions" +
                " where phone_number = ? and selected_date = ?",
                (rs, n) -> new WhatsappFarmSession(
                        rs.getString("phone_number"),
                        UUID.fromString(rs.getString("farm_id")),
                        rs.getString("farm_name"),
                        rs.getDate("selected_date").toLocalDate()
                ),
                phoneNumber,
                Date.valueOf(date)
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public void save(String phoneNumber, UUID farmId, String farmName, LocalDate date) {
        jdbcTemplate.update(
                """
                insert into whatsapp_farm_sessions (phone_number, farm_id, farm_name, selected_date)
                values (?, ?, ?, ?)
                on conflict (phone_number, selected_date)
                do update set farm_id = excluded.farm_id, farm_name = excluded.farm_name, created_at = now()
                """,
                phoneNumber,
                farmId,
                farmName,
                Date.valueOf(date)
        );
    }
}
