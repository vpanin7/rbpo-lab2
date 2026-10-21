package ru.student.lab2.model;

import java.math.BigDecimal;
import java.time.Instant;

public record Meter(Long id, Long premiseId, ResourceType resourceType, String serialNumber, BigDecimal tariff, MeterStatus status, Instant createdAt) implements Identifiable {
}
