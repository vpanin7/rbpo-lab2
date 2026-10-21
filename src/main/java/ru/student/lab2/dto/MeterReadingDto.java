package ru.student.lab2.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import ru.student.lab2.model.*;

public class MeterReadingDto {
    public record Request(Long meterId, YearMonth period, BigDecimal value, ReadingStatus status) {}
    public record Response(Long id, Long meterId, YearMonth period, BigDecimal value, ReadingStatus status, Instant createdAt) {}
}
