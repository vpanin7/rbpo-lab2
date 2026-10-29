package ru.student.lab3.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import ru.student.lab3.model.Identifiable;

public abstract class DatabaseRepository<T extends Identifiable> {
    protected final JdbcTemplate jdbc;
    private final String table;
    private final RowMapper<T> mapper;
    protected DatabaseRepository(JdbcTemplate jdbc, String table, RowMapper<T> mapper) {
        this.jdbc = jdbc; this.table = table; this.mapper = mapper;
    }
    public long nextId() {
        return jdbc.queryForObject("SELECT nextval(pg_get_serial_sequence(?, 'id'))", Long.class, table);
    }
    public List<T> findAll() { return jdbc.query("SELECT * FROM " + table + " ORDER BY id", mapper); }
    public Optional<T> findById(Long id) {
        return jdbc.query("SELECT * FROM " + table + " WHERE id = ?", mapper, id).stream().findFirst();
    }
    public void deleteById(Long id) { jdbc.update("DELETE FROM " + table + " WHERE id = ?", id); }
    public abstract T save(T item);
}
