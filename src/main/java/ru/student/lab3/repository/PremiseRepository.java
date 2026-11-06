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
public class PremiseRepository {
    private final JdbcTemplate jdbc;

    public PremiseRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long nextId() {
        return jdbc.queryForObject("SELECT nextval('premises_id_seq')", Long.class);
    }

    public List<Premise> findAll() {
        return jdbc.query("SELECT * FROM premises ORDER BY id", (rs, row) -> readRow(rs));
    }

    public Optional<Premise> findById(Long id) {
        List<Premise> items = jdbc.query("SELECT * FROM premises WHERE id = ?", (rs, row) -> readRow(rs), id);
        if (items.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(items.get(0));
    }

    public void deleteById(Long id) {
        jdbc.update("DELETE FROM premises WHERE id = ?", id);
    }

    // Преобразование строки SQL в объект Java.
    private Premise readRow(ResultSet rs) throws SQLException {
        return new Premise(rs.getLong("id"),
                rs.getString("address"), rs.getBigDecimal("area"), rs.getLong("owner_id"), rs.getTimestamp("created_at").toInstant());
    }

    public Premise save(Premise item) {
        jdbc.update("INSERT INTO premises (id, address, area, owner_id, created_at) VALUES (?, ?, ?, ?, ?) ON CONFLICT (id) DO UPDATE SET address = EXCLUDED.address, area = EXCLUDED.area, owner_id = EXCLUDED.owner_id",
                item.id(), item.address(), item.area(), item.ownerId(), Timestamp.from(item.createdAt()));
        return item;
    }
}
