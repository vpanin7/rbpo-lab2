package ru.student.lab3.model;

import java.math.BigDecimal;
import java.time.Instant;

public record Premise(Long id, String address, BigDecimal area, Long ownerId, Instant createdAt) implements Identifiable {
}
