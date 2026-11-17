package br.com.curralzap.backend.ops.infrastructure;

import br.com.curralzap.backend.ops.application.OpsFarmDetailView;
import br.com.curralzap.backend.ops.application.OpsFarmListItemView;
import br.com.curralzap.backend.ops.application.OpsMessageErrorPageView;
import br.com.curralzap.backend.ops.application.OpsMessageStatsView;
import br.com.curralzap.backend.ops.application.OpsPlatformSummaryView;
import br.com.curralzap.backend.ops.application.OpsRecentErrorView;
import br.com.curralzap.backend.ops.application.OpsUserActivityView;
import br.com.curralzap.backend.ops.application.OpsViewRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Repository
class JdbcOpsViewRepository implements OpsViewRepository {

    private final JdbcTemplate jdbcTemplate;

    JdbcOpsViewRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public OpsPlatformSummaryView loadPlatformSummary() {
        long totalFarms = coalesce(jdbcTemplate.queryForObject(
                "select count(*) from farms", Long.class));

        long[] userCounts = jdbcTemplate.queryForObject(
                """
                select
                    count(*) as total,
                    coalesce(sum(case when role = 'ADMIN' then 1 else 0 end), 0) as admin_count,
                    coalesce(sum(case when role = 'VETERINARIAN' then 1 else 0 end), 0) as vet_count,
                    coalesce(sum(case when role = 'WORKER' then 1 else 0 end), 0) as worker_count
                from users where ativo = true
                """,
                (rs, rowNum) -> new long[]{
                        rs.getLong("total"),
                        rs.getLong("admin_count"),
                        rs.getLong("vet_count"),
                        rs.getLong("worker_count")
                });

        long totalAnimals = coalesce(jdbcTemplate.queryForObject(
                "select count(*) from animals where status = 'ativo'", Long.class));

        long msg24h = coalesce(jdbcTemplate.queryForObject(
                "select count(*) from whatsapp_inbound_messages where received_at >= now() - interval '1' day",
                Long.class));

        long msg7d = coalesce(jdbcTemplate.queryForObject(
                "select count(*) from whatsapp_inbound_messages where received_at >= now() - interval '7' day",
                Long.class));

        long msg30d = coalesce(jdbcTemplate.queryForObject(
                "select count(*) from whatsapp_inbound_messages where received_at >= now() - interval '30' day",
                Long.class));

        long audioCount = coalesce(jdbcTemplate.queryForObject(
                "select count(*) from whatsapp_inbound_messages where message_type = 'audio' and received_at >= now() - interval '30' day",
                Long.class));

        long transcriptionCount = coalesce(jdbcTemplate.queryForObject(
                "select count(*) from whatsapp_transcripts where transcribed_at >= now() - interval '30' day",
                Long.class));

        long errorCount = coalesce(jdbcTemplate.queryForObject(
                "select count(*) from whatsapp_inbound_messages where status = 'FAILED' and received_at >= now() - interval '30' day",
                Long.class));

        long[] uc = userCounts != null ? userCounts : new long[]{0, 0, 0, 0};

        return new OpsPlatformSummaryView(
                totalFarms, uc[0], uc[1], uc[2], uc[3],
                totalAnimals, msg24h, msg7d, msg30d,
                audioCount, transcriptionCount, errorCount);
    }

    @Override
    public List<OpsFarmListItemView> loadFarmList() {
        return jdbcTemplate.query(
                """
                select
                    f.id as farm_id,
                    f.nome as farm_name,
                    f.created_at,
                    (select count(*) from users u where u.farm_id = f.id and u.ativo = true) as user_count,
                    (select count(*) from animals a where a.farm_id = f.id and a.status = 'ativo') as animal_count,
                    (select max(w.received_at) from whatsapp_inbound_messages w where w.farm_id = f.id) as last_whatsapp_activity,
                    (select count(*) from whatsapp_inbound_messages w
                     where w.farm_id = f.id and w.received_at >= now() - interval '7' day) as messages_last_7d,
                    (select count(*) from whatsapp_inbound_messages w
                     where w.farm_id = f.id and w.status = 'FAILED'
                     and w.received_at >= now() - interval '7' day) as errors_last_7d
                from farms f
                order by f.created_at desc
                """,
                (rs, rowNum) -> new OpsFarmListItemView(
                        rs.getObject("farm_id", UUID.class),
                        rs.getString("farm_name"),
                        toOffsetDateTime(rs.getTimestamp("created_at")),
                        rs.getLong("user_count"),
                        rs.getLong("animal_count"),
                        toOffsetDateTime(rs.getTimestamp("last_whatsapp_activity")),
                        rs.getLong("messages_last_7d"),
                        rs.getLong("errors_last_7d")
                )
        );
    }

    @Override
    public OpsFarmDetailView loadFarmDetail(UUID farmId) {
        String farmName = jdbcTemplate.queryForObject(
                "select nome from farms where id = ?", String.class, farmId);
        OffsetDateTime createdAt = jdbcTemplate.queryForObject(
                "select created_at from farms where id = ?",
                (rs, rowNum) -> toOffsetDateTime(rs.getTimestamp(1)), farmId);

        List<OpsUserActivityView> users = jdbcTemplate.query(
                """
                select
                    u.id as user_id,
                    u.nome,
                    u.role,
                    u.ativo,
                    (select max(w.received_at) from whatsapp_inbound_messages w
                     where w.farm_id = u.farm_id and w.user_id = u.id) as last_activity
                from users u where u.farm_id = ?
                order by u.nome
                """,
                (rs, rowNum) -> new OpsUserActivityView(
                        rs.getObject("user_id", UUID.class),
                        rs.getString("nome"),
                        rs.getString("role"),
                        rs.getBoolean("ativo"),
                        toOffsetDateTime(rs.getTimestamp("last_activity"))
                ),
                farmId
        );

        long[] stats = jdbcTemplate.queryForObject(
                """
                select
                    count(*) as total,
                    coalesce(sum(case when message_type = 'audio' then 1 else 0 end), 0) as audio_count,
                    coalesce(sum(case when status = 'FAILED' then 1 else 0 end), 0) as failed_count
                from whatsapp_inbound_messages
                where farm_id = ? and received_at >= now() - interval '30' day
                """,
                (rs, rowNum) -> new long[]{
                        rs.getLong("total"),
                        rs.getLong("audio_count"),
                        rs.getLong("failed_count")
                },
                farmId);

        long transcribedCount = coalesce(jdbcTemplate.queryForObject(
                """
                select count(*) from whatsapp_transcripts t
                join whatsapp_inbound_messages m on m.id = t.inbound_message_id
                where m.farm_id = ? and t.transcribed_at >= now() - interval '30' day
                """,
                Long.class, farmId));

        List<OpsRecentErrorView> recentErrors = jdbcTemplate.query(
                """
                select id, farm_id, user_id, failure_reason, received_at
                from whatsapp_inbound_messages
                where farm_id = ? and status = 'FAILED'
                order by received_at desc
                limit 10
                """,
                (rs, rowNum) -> new OpsRecentErrorView(
                        rs.getObject("id", UUID.class),
                        rs.getObject("farm_id", UUID.class),
                        rs.getObject("user_id", UUID.class),
                        rs.getString("failure_reason"),
                        toOffsetDateTime(rs.getTimestamp("received_at"))
                ),
                farmId
        );

        long[] s = stats != null ? stats : new long[]{0, 0, 0};
        return new OpsFarmDetailView(
                farmId, farmName, createdAt,
                users, s[0], s[1], transcribedCount, s[2],
                recentErrors);
    }

    @Override
    public OpsMessageStatsView loadMessageStats(String period) {
        String interval = switch (period) {
            case "24h" -> "1";
            case "30d" -> "30";
            default -> "7";
        };

        long[] stats = jdbcTemplate.queryForObject(
                """
                select
                    count(*) as total,
                    coalesce(sum(case when message_type = 'audio' then 1 else 0 end), 0) as audio_count,
                    coalesce(sum(case when message_type != 'audio' then 1 else 0 end), 0) as text_count,
                    coalesce(sum(case when status = 'APPLIED' then 1 else 0 end), 0) as applied_count,
                    coalesce(sum(case when status = 'FAILED' then 1 else 0 end), 0) as failed_count
                from whatsapp_inbound_messages
                where received_at >= now() - interval '""" + interval + """
                 ' day
                """,
                (rs, rowNum) -> new long[]{
                        rs.getLong("total"),
                        rs.getLong("audio_count"),
                        rs.getLong("text_count"),
                        rs.getLong("applied_count"),
                        rs.getLong("failed_count")
                });

        long transcribed = coalesce(jdbcTemplate.queryForObject(
                "select count(*) from whatsapp_transcripts where transcribed_at >= now() - interval '"
                        + interval + "' day",
                Long.class));

        long[] s = stats != null ? stats : new long[]{0, 0, 0, 0, 0};
        return new OpsMessageStatsView(period, s[0], s[1], s[2], transcribed, s[3], s[4]);
    }

    @Override
    public OpsMessageErrorPageView loadMessageErrors(int limit, int offset) {
        long total = coalesce(jdbcTemplate.queryForObject(
                "select count(*) from whatsapp_inbound_messages where status = 'FAILED'",
                Long.class));

        List<OpsRecentErrorView> errors = jdbcTemplate.query(
                """
                select id, farm_id, user_id, failure_reason, received_at
                from whatsapp_inbound_messages
                where status = 'FAILED'
                order by received_at desc
                limit ? offset ?
                """,
                (rs, rowNum) -> new OpsRecentErrorView(
                        rs.getObject("id", UUID.class),
                        rs.getObject("farm_id", UUID.class),
                        rs.getObject("user_id", UUID.class),
                        rs.getString("failure_reason"),
                        toOffsetDateTime(rs.getTimestamp("received_at"))
                ),
                limit, offset
        );

        return new OpsMessageErrorPageView(errors, total, limit, offset);
    }

    private static long coalesce(Long value) {
        return value != null ? value : 0L;
    }

    private static OffsetDateTime toOffsetDateTime(Timestamp ts) {
        if (ts == null) return null;
        return ts.toInstant().atOffset(ZoneOffset.UTC);
    }
}
