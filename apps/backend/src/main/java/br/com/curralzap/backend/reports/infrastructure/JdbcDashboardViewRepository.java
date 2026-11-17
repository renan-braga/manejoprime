package br.com.curralzap.backend.reports.infrastructure;

import br.com.curralzap.backend.reports.application.DashboardAggregateView;
import br.com.curralzap.backend.reports.application.DashboardStatusBreakdownView;
import br.com.curralzap.backend.reports.application.DashboardViewRepository;
import br.com.curralzap.backend.reports.application.LotBreakdownItemView;
import br.com.curralzap.backend.reports.application.WeightTrendItemView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
class JdbcDashboardViewRepository implements DashboardViewRepository {

    private final JdbcTemplate jdbcTemplate;

    JdbcDashboardViewRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DashboardAggregateView loadFor(UUID farmId) {
        DashboardStatusBreakdownView statusBreakdown = jdbcTemplate.queryForObject(
                """
                select
                    coalesce(sum(case when status = 'ativo' then 1 else 0 end), 0)        as active_animals,
                    coalesce(sum(case when status = 'vendido' then 1 else 0 end), 0)       as sold_animals,
                    coalesce(sum(case when status = 'morto' then 1 else 0 end), 0)         as dead_animals,
                    coalesce(sum(case when status = 'transferido' then 1 else 0 end), 0)   as transferred_animals
                from animals
                where farm_id = ?
                """,
                (rs, rowNum) -> new DashboardStatusBreakdownView(
                        rs.getLong("active_animals"),
                        rs.getLong("sold_animals"),
                        rs.getLong("dead_animals"),
                        rs.getLong("transferred_animals")
                ),
                farmId
        );

        long totalAnimals = coalesce(jdbcTemplate.queryForObject(
                "select count(*) from animals where farm_id = ?", Long.class, farmId));

        long prevTotalAnimals = coalesce(jdbcTemplate.queryForObject(
                "select count(*) from animals where farm_id = ? and created_at <= now() - interval '30' day",
                Long.class, farmId));

        long prevActiveAnimals = coalesce(jdbcTemplate.queryForObject(
                "select count(*) from animals where farm_id = ? and status = 'ativo' and created_at <= now() - interval '30' day",
                Long.class, farmId));

        long lotsCount = coalesce(jdbcTemplate.queryForObject(
                "select count(*) from lots where farm_id = ?", Long.class, farmId));

        long prevLotsCount = coalesce(jdbcTemplate.queryForObject(
                "select count(*) from lots where farm_id = ? and created_at <= now() - interval '30' day",
                Long.class, farmId));

        long activeUsers = coalesce(jdbcTemplate.queryForObject(
                "select count(*) from users where farm_id = ? and ativo = true", Long.class, farmId));

        long prevActiveUsers = coalesce(jdbcTemplate.queryForObject(
                "select count(*) from users where farm_id = ? and ativo = true and created_at <= now() - interval '30' day",
                Long.class, farmId));

        BigDecimal avgDailyGain = jdbcTemplate.queryForObject(
                """
                select coalesce(avg(gmd_per_animal), 0)
                from (
                    select animal_id,
                           (max(peso_kg) - min(peso_kg)) /
                           greatest(extract(day from cast(max(data_weighing) as timestamp) - cast(min(data_weighing) as timestamp)), 1) as gmd_per_animal
                    from weighings
                    where farm_id = ?
                      and data_weighing >= current_date - interval '30' day
                    group by animal_id
                    having count(*) >= 2 and max(data_weighing) > min(data_weighing)
                ) t
                """,
                BigDecimal.class, farmId);

        BigDecimal prevAvgDailyGain = jdbcTemplate.queryForObject(
                """
                select coalesce(avg(gmd_per_animal), 0)
                from (
                    select animal_id,
                           (max(peso_kg) - min(peso_kg)) /
                           greatest(extract(day from cast(max(data_weighing) as timestamp) - cast(min(data_weighing) as timestamp)), 1) as gmd_per_animal
                    from weighings
                    where farm_id = ?
                      and data_weighing >= current_date - interval '60' day
                      and data_weighing < current_date - interval '30' day
                    group by animal_id
                    having count(*) >= 2 and max(data_weighing) > min(data_weighing)
                ) t
                """,
                BigDecimal.class, farmId);

        return new DashboardAggregateView(
                totalAnimals, prevTotalAnimals,
                lotsCount, prevLotsCount,
                activeUsers, prevActiveUsers,
                avgDailyGain != null ? avgDailyGain : BigDecimal.ZERO,
                prevAvgDailyGain != null ? prevAvgDailyGain : BigDecimal.ZERO,
                statusBreakdown != null ? statusBreakdown : new DashboardStatusBreakdownView(0, 0, 0, 0),
                prevActiveAnimals
        );
    }

    @Override
    public List<WeightTrendItemView> loadWeightTrend(UUID farmId) {
        LocalDate cutoff = LocalDate.now().minusDays(30);
        return jdbcTemplate.query(
                """
                with daily_avg as (
                    select data_weighing as dt, avg(peso_kg) as avg_weight
                    from weighings
                    where farm_id = ? and data_weighing >= ?
                    group by data_weighing
                ),
                with_lag as (
                    select dt,
                           avg_weight,
                           lag(avg_weight) over (order by dt) as prev_weight,
                           lag(dt)         over (order by dt) as prev_dt
                    from daily_avg
                )
                select
                    dt,
                    avg_weight,
                    case
                        when prev_dt is not null and dt > prev_dt
                        then (avg_weight - prev_weight) / extract(day from cast(dt as timestamp) - cast(prev_dt as timestamp))
                    end as avg_daily_gain
                from with_lag
                order by dt
                """,
                (rs, rowNum) -> new WeightTrendItemView(
                        rs.getDate("dt").toLocalDate(),
                        rs.getBigDecimal("avg_weight"),
                        rs.getBigDecimal("avg_daily_gain")
                ),
                farmId,
                Date.valueOf(cutoff)
        );
    }

    @Override
    public List<LotBreakdownItemView> loadLotBreakdown(UUID farmId) {
        return jdbcTemplate.query(
                """
                with ranked_weighings as (
                    select animal_id, peso_kg,
                           row_number() over (
                               partition by animal_id
                               order by data_weighing desc, created_at desc
                           ) as rn
                    from weighings
                    where farm_id = ?
                )
                select
                    l.id        as lot_id,
                    l.nome      as lot_name,
                    count(a.id) as head_count,
                    avg(lw.peso_kg) as avg_weight
                from lots l
                left join animals a
                    on a.lot_id = l.id and a.farm_id = l.farm_id and a.status = 'ativo'
                left join ranked_weighings lw
                    on lw.animal_id = a.id and lw.rn = 1
                where l.farm_id = ?
                group by l.id, l.nome
                order by l.nome
                """,
                (rs, rowNum) -> new LotBreakdownItemView(
                        rs.getObject("lot_id", UUID.class),
                        rs.getString("lot_name"),
                        rs.getLong("head_count"),
                        rs.getBigDecimal("avg_weight")
                ),
                farmId,
                farmId
        );
    }

    private static long coalesce(Long value) {
        return value != null ? value : 0L;
    }
}
