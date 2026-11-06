package ru.student.lab3.repository;

import java.time.YearMonth;
import java.sql.Timestamp;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.student.lab3.model.*;

@Repository
public class UtilityChargeRepository {
    private final JdbcTemplate jdbc;

    public UtilityChargeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long nextId() {
        return jdbc.queryForObject("SELECT nextval('utility_charges_id_seq')", Long.class);
    }

    public List<UtilityCharge> findAll() {
        return jdbc.query("SELECT * FROM utility_charges ORDER BY id", (rs, row) -> readRow(rs));
    }

    public Optional<UtilityCharge> findById(Long id) {
        List<UtilityCharge> items = jdbc.query("SELECT * FROM utility_charges WHERE id = ?", (rs, row) -> readRow(rs), id);
        if (items.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(items.get(0));
    }

    public void deleteById(Long id) {
        jdbc.update("DELETE FROM utility_charges WHERE id = ?", id);
    }

    // Преобразование строки SQL в объект Java.
    private UtilityCharge readRow(ResultSet rs) throws SQLException {
        return new UtilityCharge(rs.getLong("id"),
                rs.getLong("premise_id"), rs.getLong("meter_id"), YearMonth.parse(rs.getString("period")), rs.getBigDecimal("volume"), rs.getBigDecimal("amount"), ChargeStatus.valueOf(rs.getString("status")), rs.getTimestamp("created_at").toInstant());
    }

    public UtilityCharge save(UtilityCharge item) {
        jdbc.update("INSERT INTO utility_charges (id, premise_id, meter_id, period, volume, amount, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (id) DO UPDATE SET premise_id = EXCLUDED.premise_id, meter_id = EXCLUDED.meter_id, period = EXCLUDED.period, volume = EXCLUDED.volume, amount = EXCLUDED.amount, status = EXCLUDED.status",
                item.id(), item.premiseId(), item.meterId(), item.period().toString(), item.volume(), item.amount(), item.status().toString(), Timestamp.from(item.createdAt()));
        return item;
    }
}
