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
public class UserRepository {
    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long nextId() {
        return jdbc.queryForObject("SELECT nextval('app_users_id_seq')", Long.class);
    }

    public List<User> findAll() {
        return jdbc.query("SELECT * FROM app_users ORDER BY id", (rs, row) -> readRow(rs));
    }

    public Optional<User> findById(Long id) {
        List<User> items = jdbc.query("SELECT * FROM app_users WHERE id = ?", (rs, row) -> readRow(rs), id);
        if (items.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(items.get(0));
    }

    public void deleteById(Long id) {
        jdbc.update("DELETE FROM app_users WHERE id = ?", id);
    }

    // Преобразование строки SQL в объект Java.
    private User readRow(ResultSet rs) throws SQLException {
        return new User(rs.getLong("id"),
                rs.getString("name"), rs.getString("email"), rs.getTimestamp("created_at").toInstant());
    }

    public User save(User item) {
        jdbc.update("INSERT INTO app_users (id, name, email, created_at) VALUES (?, ?, ?, ?) ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, email = EXCLUDED.email",
                item.id(), item.name(), item.email(), Timestamp.from(item.createdAt()));
        return item;
    }
}
