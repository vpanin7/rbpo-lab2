package ru.student.lab3.repository;

import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.student.lab3.model.*;

@Repository
public class PremiseRepository extends DatabaseRepository<Premise> {
    public PremiseRepository(JdbcTemplate jdbc) {
        super(jdbc, "premises", (rs, row) -> new Premise(rs.getLong("id"),
                rs.getString("address"), rs.getBigDecimal("area"), rs.getLong("owner_id"), rs.getTimestamp("created_at").toInstant()));
    }
    @Override
    public Premise save(Premise item) {
        jdbc.update("INSERT INTO premises (id, address, area, owner_id, created_at) VALUES (?, ?, ?, ?, ?) ON CONFLICT (id) DO UPDATE SET address = EXCLUDED.address, area = EXCLUDED.area, owner_id = EXCLUDED.owner_id",
                item.id(), item.address(), item.area(), item.ownerId(), Timestamp.from(item.createdAt()));
        return item;
    }
}
