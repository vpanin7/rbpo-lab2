package ru.student.lab2.dto;

import java.math.BigDecimal;
import java.time.Instant;
import ru.student.lab2.model.*;

public class PremiseDto {
    public record Request(String address, BigDecimal area, Long ownerId) {}
    public record Response(Long id, String address, BigDecimal area, Long ownerId, Instant createdAt) {}
}
