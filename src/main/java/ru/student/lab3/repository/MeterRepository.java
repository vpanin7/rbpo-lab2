package ru.student.lab3.repository;

import java.sql.Timestamp;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.student.lab3.model.*;

@Repository
public class MeterRepository {
    private final JdbcTemplate jdbc;

    public MeterRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long nextId() {
        return jdbc.queryForObject("SELECT nextval('meters_id_seq')", Long.class);
    }

    public List<Meter> findAll() {
        return jdbc.query("SELECT * FROM meters ORDER BY id", (rs, row) -> readRow(rs));
    }

    public Optional<Meter> findById(Long id) {
        List<Meter> items = jdbc.query("SELECT * FROM meters WHERE id = ?", (rs, row) -> readRow(rs), id);
        if (items.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(items.get(0));
    }

    public void deleteById(Long id) {
        jdbc.update("DELETE FROM meters WHERE id = ?", id);
    }

    // Преобразование строки SQL в объект Java.
    private Meter readRow(ResultSet rs) throws SQLException {
        return new Meter(rs.getLong("id"),
                rs.getLong("premise_id"), ResourceType.valueOf(rs.getString("resource_type")), rs.getString("serial_number"), rs.getBigDecimal("tariff"), MeterStatus.valueOf(rs.getString("status")), rs.getTimestamp("created_at").toInstant());
    }

    public Meter save(Meter item) {
        jdbc.update("INSERT INTO meters (id, premise_id, resource_type, serial_number, tariff, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (id) DO UPDATE SET premise_id = EXCLUDED.premise_id, resource_type = EXCLUDED.resource_type, serial_number = EXCLUDED.serial_number, tariff = EXCLUDED.tariff, status = EXCLUDED.status",
                item.id(), item.premiseId(), item.resourceType().toString(), item.serialNumber(), item.tariff(), item.status().toString(), Timestamp.from(item.createdAt()));
        return item;
    }
}
