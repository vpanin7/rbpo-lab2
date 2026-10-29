package ru.student.lab3.repository;

import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.student.lab3.model.*;

@Repository
public class UserRepository extends DatabaseRepository<User> {
    public UserRepository(JdbcTemplate jdbc) {
        super(jdbc, "app_users", (rs, row) -> new User(rs.getLong("id"),
                rs.getString("name"), rs.getString("email"), rs.getTimestamp("created_at").toInstant()));
    }
    @Override
    public User save(User item) {
        jdbc.update("INSERT INTO app_users (id, name, email, created_at) VALUES (?, ?, ?, ?) ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, email = EXCLUDED.email",
                item.id(), item.name(), item.email(), Timestamp.from(item.createdAt()));
        return item;
    }
}
