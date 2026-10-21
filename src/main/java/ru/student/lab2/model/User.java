package ru.student.lab2.model;

import java.time.Instant;

public record User(Long id, String name, String email, Instant createdAt) implements Identifiable {
}
