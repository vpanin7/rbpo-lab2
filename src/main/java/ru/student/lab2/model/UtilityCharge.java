package ru.student.lab2.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;

public record UtilityCharge(Long id, Long premiseId, Long meterId, YearMonth period, BigDecimal volume, BigDecimal amount, ChargeStatus status, Instant createdAt) implements Identifiable {
}
