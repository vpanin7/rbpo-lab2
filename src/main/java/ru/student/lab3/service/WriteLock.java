package ru.student.lab3.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class WriteLock {
    private final JdbcTemplate jdbc;
    public WriteLock(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public void acquire() { jdbc.execute("SELECT pg_advisory_xact_lock(24054)"); }
}
