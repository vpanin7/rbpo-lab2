package ru.student.lab3.service;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.student.lab3.dto.*;
import ru.student.lab3.model.*;
import ru.student.lab3.exception.ApiException;

@Service
@Transactional
public class UtilityOperationsService {
    private final UserService users;
    private final PremiseService premises;
    private final MeterService meters;
    private final MeterReadingService readings;
    private final UtilityChargeService charges;
    private final WriteLock lock;

    public UtilityOperationsService(UserService users, PremiseService premises, MeterService meters,
            MeterReadingService readings, UtilityChargeService charges, WriteLock lock) {
        this.users = users; this.premises = premises; this.meters = meters;
        this.readings = readings; this.charges = charges; this.lock = lock;
    }

    public MeterReading verifyReading(Long id) {
        lock.acquire();
        MeterReading reading = readings.getById(id);
        Meter meter = meters.getById(reading.meterId());
        if (reading.status() != ReadingStatus.PENDING || meter.status() != MeterStatus.ACTIVE) {
            throw ApiException.conflict("Проверить можно только ожидающее показание активного счётчика");
        }
        return readings.update(id, new MeterReadingDto.Request(null, null, null, ReadingStatus.VERIFIED));
    }

    public List<UtilityCharge> billPremise(Long id, YearMonth period) {
        lock.acquire();
        premises.getById(id);
        Checks.required(period, "period");
        if (!period.isBefore(YearMonth.now())) throw ApiException.badRequest("Месяц ещё не завершён");
        Instant end = period.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        List<Meter> active = meters.getAll().stream().filter(m -> m.premiseId().equals(id)
                && m.status() == MeterStatus.ACTIVE && m.createdAt().isBefore(end)).toList();
        if (active.isEmpty()) throw ApiException.conflict("Нет активных счётчиков за этот период");
        List<UtilityCharge> result = new ArrayList<>();
        for (Meter meter : active) {
            result.add(charges.create(new UtilityChargeDto.Request(meter.id(), period, ChargeStatus.UNPAID)));
        }
        return result;
    }

    public record PaymentResult(Long premiseId, YearMonth period, BigDecimal paidAmount, int chargeCount, Instant paidAt) {}
    public PaymentResult payPremise(Long id, YearMonth period, Long ownerId, BigDecimal amount) {
        lock.acquire();
        Premise premise = premises.getById(id);
        Checks.required(period, "period");
        Checks.required(ownerId, "ownerId");
        Checks.nonNegative(amount, "amount");
        if (!premise.ownerId().equals(ownerId)) throw ApiException.conflict("Плательщик не является владельцем");
        List<UtilityCharge> unpaid = charges.getAll().stream().filter(c -> c.premiseId().equals(id)
                && c.period().equals(period) && c.status() == ChargeStatus.UNPAID).toList();
        if (unpaid.isEmpty()) throw ApiException.conflict("Нет неоплаченных начислений за этот период");
        BigDecimal total = unpaid.stream().map(UtilityCharge::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (amount.compareTo(total) != 0) throw ApiException.badRequest("Сумма оплаты должна равняться " + total);
        for (UtilityCharge charge : unpaid) {
            charges.update(charge.id(), new UtilityChargeDto.Request(null, null, ChargeStatus.PAID));
        }
        return new PaymentResult(id, period, total, unpaid.size(), Instant.now());
    }

    public Premise transferOwnership(Long id, Long newOwnerId) {
        lock.acquire();
        Checks.required(newOwnerId, "newOwnerId");
        Premise old = premises.getById(id);
        users.getById(newOwnerId);
        if (old.ownerId().equals(newOwnerId)) throw ApiException.conflict("Пользователь уже владеет помещением");
        if (charges.getAll().stream().anyMatch(c -> c.premiseId().equals(id) && c.status() == ChargeStatus.UNPAID)) {
            throw ApiException.conflict("Сначала погасите все начисления помещения");
        }
        return premises.update(id, new PremiseDto.Request(null, null, newOwnerId));
    }

    public record ReplacementResult(Meter oldMeter, Meter newMeter, MeterReading initialReading) {}
    public ReplacementResult replaceMeter(Long id, String serialNumber, BigDecimal tariff) {
        lock.acquire();
        Meter old = meters.getById(id);
        if (old.status() != MeterStatus.ACTIVE) throw ApiException.conflict("Счётчик уже отключён");
        Checks.text(serialNumber, "serialNumber");
        Checks.nonNegative(tariff, "tariff");
        YearMonth previous = YearMonth.now().minusMonths(1);
        MeterReading last = readings.getAll().stream().filter(r -> r.meterId().equals(id))
                .max(Comparator.comparing(MeterReading::period)).orElseThrow(() -> ApiException.conflict("Нет показаний счётчика"));
        if (!last.period().equals(previous) || last.status() != ReadingStatus.VERIFIED) {
            throw ApiException.conflict("Нужно проверенное последнее показание за предыдущий месяц, без более поздних показаний");
        }
        boolean settled = charges.getAll().stream().anyMatch(c -> c.meterId().equals(id)
                && c.period().equals(previous) && c.status() == ChargeStatus.PAID);
        if (!settled || charges.getAll().stream().anyMatch(c -> c.meterId().equals(id) && c.status() == ChargeStatus.UNPAID)) {
            throw ApiException.conflict("Перед заменой рассчитайте и оплатите начисления старого счётчика");
        }
        Meter disabled = meters.update(id, new MeterDto.Request(null, null, null, null, MeterStatus.INACTIVE));
        Meter installed = meters.create(new MeterDto.Request(old.premiseId(), old.resourceType(), serialNumber, tariff, MeterStatus.ACTIVE));
        MeterReading baseline = readings.create(new MeterReadingDto.Request(installed.id(), previous, BigDecimal.ZERO, ReadingStatus.VERIFIED));
        return new ReplacementResult(disabled, installed, baseline);
    }
}
