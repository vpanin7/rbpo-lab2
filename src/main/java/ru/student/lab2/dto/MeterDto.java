package ru.student.lab2.dto;

import java.math.BigDecimal;
import java.time.Instant;
import ru.student.lab2.model.*;

public class MeterDto {
    public record Request(Long premiseId, ResourceType resourceType, String serialNumber, BigDecimal tariff, MeterStatus status) {}
    public record Response(Long id, Long premiseId, ResourceType resourceType, String serialNumber, BigDecimal tariff, MeterStatus status, Instant createdAt) {}
}
