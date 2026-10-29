package ru.student.lab3.repository;

import java.time.YearMonth;
import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.student.lab3.model.*;

@Repository
public class UtilityChargeRepository extends DatabaseRepository<UtilityCharge> {
    public UtilityChargeRepository(JdbcTemplate jdbc) {
        super(jdbc, "utility_charges", (rs, row) -> new UtilityCharge(rs.getLong("id"),
                rs.getLong("premise_id"), rs.getLong("meter_id"), YearMonth.parse(rs.getString("period")), rs.getBigDecimal("volume"), rs.getBigDecimal("amount"), ChargeStatus.valueOf(rs.getString("status")), rs.getTimestamp("created_at").toInstant()));
    }
    @Override
    public UtilityCharge save(UtilityCharge item) {
        jdbc.update("INSERT INTO utility_charges (id, premise_id, meter_id, period, volume, amount, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (id) DO UPDATE SET premise_id = EXCLUDED.premise_id, meter_id = EXCLUDED.meter_id, period = EXCLUDED.period, volume = EXCLUDED.volume, amount = EXCLUDED.amount, status = EXCLUDED.status",
                item.id(), item.premiseId(), item.meterId(), item.period().toString(), item.volume(), item.amount(), item.status().toString(), Timestamp.from(item.createdAt()));
        return item;
    }
}
