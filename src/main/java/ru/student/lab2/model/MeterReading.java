package ru.student.lab2.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;

public record MeterReading(Long id, Long meterId, YearMonth period, BigDecimal value, ReadingStatus status, Instant createdAt) implements Identifiable {
}
