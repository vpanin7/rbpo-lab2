package ru.student.lab3.service;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import ru.student.lab3.dto.*;
import ru.student.lab3.model.*;
import ru.student.lab3.exception.ApiException;

@Service
// При ошибке все изменения операции откатываются.
// SERIALIZABLE не даёт параллельным запросам провести противоречивые изменения.
@Transactional(isolation = Isolation.SERIALIZABLE)
public class UtilityOperationsService {
    private final UserService users;
    private final PremiseService premises;
    private final MeterService meters;
    private final MeterReadingService readings;
    private final UtilityChargeService charges;

    public UtilityOperationsService(UserService users, PremiseService premises, MeterService meters,
            MeterReadingService readings, UtilityChargeService charges) {
        this.users = users; this.premises = premises; this.meters = meters;
        this.readings = readings; this.charges = charges;
    }

    public MeterReading verifyReading(Long id) {
        MeterReading reading = readings.getById(id);
        Meter meter = meters.getById(reading.meterId());
        if (reading.status() != ReadingStatus.PENDING || meter.status() != MeterStatus.ACTIVE) {
            throw ApiException.conflict("Проверить можно только ожидающее показание активного счётчика");
        }
        return readings.update(id, new MeterReadingDto.Request(null, null, null, ReadingStatus.VERIFIED));
    }

    public List<UtilityCharge> billPremise(Long id, YearMonth period) {
        premises.getById(id);
        Checks.required(period, "period");
        if (!period.isBefore(YearMonth.now())) throw ApiException.badRequest("Месяц ещё не завершён");
        Instant end = period.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        List<Meter> active = new ArrayList<>();
        for (Meter meter : meters.getAll()) {
            if (meter.premiseId().equals(id) && meter.status() == MeterStatus.ACTIVE && meter.createdAt().isBefore(end)) {
                active.add(meter);
            }
        }
        if (active.isEmpty()) throw ApiException.conflict("Нет активных счётчиков за этот период");
        List<UtilityCharge> result = new ArrayList<>();
        for (Meter meter : active) {
            result.add(charges.create(new UtilityChargeDto.Request(meter.id(), period, ChargeStatus.UNPAID)));
        }
        return result;
    }

    public record PaymentResult(Long premiseId, YearMonth period, BigDecimal paidAmount, int chargeCount, Instant paidAt) {}
    public PaymentResult payPremise(Long id, YearMonth period, Long ownerId, BigDecimal amount) {
        Premise premise = premises.getById(id);
        Checks.required(period, "period");
        Checks.required(ownerId, "ownerId");
        Checks.nonNegative(amount, "amount");
        if (!premise.ownerId().equals(ownerId)) throw new org.springframework.security.access.AccessDeniedException("Недостаточно прав");
        List<UtilityCharge> unpaid = new ArrayList<>();
        for (UtilityCharge charge : charges.getAll()) {
            if (charge.premiseId().equals(id) && charge.period().equals(period) && charge.status() == ChargeStatus.UNPAID) {
                unpaid.add(charge);
            }
        }
        if (unpaid.isEmpty()) throw ApiException.conflict("Нет неоплаченных начислений за этот период");
        BigDecimal total = BigDecimal.ZERO;
        for (UtilityCharge charge : unpaid) {
            total = total.add(charge.amount());
        }
        if (amount.compareTo(total) != 0) throw ApiException.badRequest("Сумма оплаты должна равняться " + total);
        for (UtilityCharge charge : unpaid) {
            charges.update(charge.id(), new UtilityChargeDto.Request(null, null, ChargeStatus.PAID));
        }
        return new PaymentResult(id, period, total, unpaid.size(), Instant.now());
    }

    public Premise transferOwnership(Long id, Long newOwnerId) {
        Checks.required(newOwnerId, "newOwnerId");
        Premise old = premises.getById(id);
        users.getById(newOwnerId);
        if (old.ownerId().equals(newOwnerId)) throw ApiException.conflict("Пользователь уже владеет помещением");
        for (UtilityCharge charge : charges.getAll()) {
            if (charge.premiseId().equals(id) && charge.status() == ChargeStatus.UNPAID) {
                throw ApiException.conflict("Сначала погасите все начисления помещения");
            }
        }
        return premises.update(id, new PremiseDto.Request(null, null, newOwnerId));
    }

    public record ReplacementResult(Meter oldMeter, Meter newMeter, MeterReading initialReading) {}
    public ReplacementResult replaceMeter(Long id, String serialNumber, BigDecimal tariff) {
        Meter old = meters.getById(id);
        if (old.status() != MeterStatus.ACTIVE) throw ApiException.conflict("Счётчик уже отключён");
        Checks.text(serialNumber, "serialNumber");
        Checks.nonNegative(tariff, "tariff");
        YearMonth previous = YearMonth.now().minusMonths(1);
        MeterReading last = null;
        for (MeterReading reading : readings.getAll()) {
            if (reading.meterId().equals(id) && (last == null || reading.period().isAfter(last.period()))) {
                last = reading;
            }
        }
        if (last == null) {
            throw ApiException.conflict("Нет показаний счётчика");
        }
        if (!last.period().equals(previous) || last.status() != ReadingStatus.VERIFIED) {
            throw ApiException.conflict("Нужно проверенное последнее показание за предыдущий месяц, без более поздних показаний");
        }
        boolean settled = false;
        for (UtilityCharge charge : charges.getAll()) {
            if (!charge.meterId().equals(id)) {
                continue;
            }
            if (charge.status() == ChargeStatus.UNPAID) {
                throw ApiException.conflict("Сначала оплатите начисления старого счётчика");
            }
            if (charge.period().equals(previous) && charge.status() == ChargeStatus.PAID) {
                settled = true;
            }
        }
        if (!settled) {
            throw ApiException.conflict("Сначала рассчитайте и оплатите предыдущий месяц");
        }
        // Все три действия входят в одну транзакцию: либо сохранятся все, либо ни одно.
        Meter disabled = meters.update(id, new MeterDto.Request(null, null, null, null, MeterStatus.INACTIVE));
        Meter installed = meters.create(new MeterDto.Request(old.premiseId(), old.resourceType(), serialNumber, tariff, MeterStatus.ACTIVE));
        MeterReading baseline = readings.create(new MeterReadingDto.Request(installed.id(), previous, BigDecimal.ZERO, ReadingStatus.VERIFIED));
        return new ReplacementResult(disabled, installed, baseline);
    }
}
