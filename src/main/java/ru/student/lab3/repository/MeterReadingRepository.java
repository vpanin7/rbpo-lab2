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
public class MeterReadingRepository {
    private final JdbcTemplate jdbc;

    public MeterReadingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long nextId() {
        return jdbc.queryForObject("SELECT nextval('meter_readings_id_seq')", Long.class);
    }

    public List<MeterReading> findAll() {
        return jdbc.query("SELECT * FROM meter_readings ORDER BY id", (rs, row) -> readRow(rs));
    }

    public Optional<MeterReading> findById(Long id) {
        List<MeterReading> items = jdbc.query("SELECT * FROM meter_readings WHERE id = ?", (rs, row) -> readRow(rs), id);
        if (items.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(items.get(0));
    }

    public void deleteById(Long id) {
        jdbc.update("DELETE FROM meter_readings WHERE id = ?", id);
    }

    // Преобразование строки SQL в объект Java.
    private MeterReading readRow(ResultSet rs) throws SQLException {
        return new MeterReading(rs.getLong("id"),
                rs.getLong("meter_id"), YearMonth.parse(rs.getString("period")), rs.getBigDecimal("value"), ReadingStatus.valueOf(rs.getString("status")), rs.getTimestamp("created_at").toInstant());
    }

    public MeterReading save(MeterReading item) {
        jdbc.update("INSERT INTO meter_readings (id, meter_id, period, value, status, created_at) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (id) DO UPDATE SET meter_id = EXCLUDED.meter_id, period = EXCLUDED.period, value = EXCLUDED.value, status = EXCLUDED.status",
                item.id(), item.meterId(), item.period().toString(), item.value(), item.status().toString(), Timestamp.from(item.createdAt()));
        return item;
    }
}
