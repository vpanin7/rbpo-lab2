package ru.student.lab3.repository;

import java.time.YearMonth;
import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.student.lab3.model.*;

@Repository
public class MeterReadingRepository extends DatabaseRepository<MeterReading> {
    public MeterReadingRepository(JdbcTemplate jdbc) {
        super(jdbc, "meter_readings", (rs, row) -> new MeterReading(rs.getLong("id"),
                rs.getLong("meter_id"), YearMonth.parse(rs.getString("period")), rs.getBigDecimal("value"), ReadingStatus.valueOf(rs.getString("status")), rs.getTimestamp("created_at").toInstant()));
    }
    @Override
    public MeterReading save(MeterReading item) {
        jdbc.update("INSERT INTO meter_readings (id, meter_id, period, value, status, created_at) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (id) DO UPDATE SET meter_id = EXCLUDED.meter_id, period = EXCLUDED.period, value = EXCLUDED.value, status = EXCLUDED.status",
                item.id(), item.meterId(), item.period().toString(), item.value(), item.status().toString(), Timestamp.from(item.createdAt()));
        return item;
    }
}
