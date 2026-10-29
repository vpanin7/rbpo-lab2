package ru.student.lab3.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import ru.student.lab3.model.*;

public class UtilityChargeDto {
    public record Request(Long meterId, YearMonth period, ChargeStatus status) {}
    public record Response(Long id, Long premiseId, Long meterId, YearMonth period, BigDecimal volume, BigDecimal amount, ChargeStatus status, Instant createdAt) {}
}
