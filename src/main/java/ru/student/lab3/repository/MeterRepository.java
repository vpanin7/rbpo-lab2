package ru.student.lab3.repository;

import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.student.lab3.model.*;

@Repository
public class MeterRepository extends DatabaseRepository<Meter> {
    public MeterRepository(JdbcTemplate jdbc) {
        super(jdbc, "meters", (rs, row) -> new Meter(rs.getLong("id"),
                rs.getLong("premise_id"), ResourceType.valueOf(rs.getString("resource_type")), rs.getString("serial_number"), rs.getBigDecimal("tariff"), MeterStatus.valueOf(rs.getString("status")), rs.getTimestamp("created_at").toInstant()));
    }
    @Override
    public Meter save(Meter item) {
        jdbc.update("INSERT INTO meters (id, premise_id, resource_type, serial_number, tariff, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (id) DO UPDATE SET premise_id = EXCLUDED.premise_id, resource_type = EXCLUDED.resource_type, serial_number = EXCLUDED.serial_number, tariff = EXCLUDED.tariff, status = EXCLUDED.status",
                item.id(), item.premiseId(), item.resourceType().toString(), item.serialNumber(), item.tariff(), item.status().toString(), Timestamp.from(item.createdAt()));
        return item;
    }
}
