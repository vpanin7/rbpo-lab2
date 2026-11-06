package ru.student.lab3.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import ru.student.lab3.dto.UtilityChargeDto;
import ru.student.lab3.model.*;
import ru.student.lab3.repository.*;
import ru.student.lab3.exception.ApiException;

@Service
// При ошибке все изменения операции откатываются.
// SERIALIZABLE не даёт параллельным запросам провести противоречивые изменения.
@Transactional(isolation = Isolation.SERIALIZABLE)
public class UtilityChargeService {
    private final UtilityChargeRepository utilityChargeRepository;
    private final MeterRepository meterRepository;
    private final MeterReadingRepository meterReadingRepository;

    public UtilityChargeService(UtilityChargeRepository utilityChargeRepository, MeterRepository meterRepository, MeterReadingRepository meterReadingRepository) {
        this.utilityChargeRepository = utilityChargeRepository;
        this.meterRepository = meterRepository;
        this.meterReadingRepository = meterReadingRepository;
    }

    public List<UtilityCharge> getAll() { return utilityChargeRepository.findAll(); }
    public UtilityCharge getById(Long id) {
        return utilityChargeRepository.findById(id).orElseThrow(ApiException::notFound);
    }

    public UtilityCharge create(UtilityChargeDto.Request request) {
            return calculate(utilityChargeRepository.nextId(), request.meterId(), request.period(),
                Checks.choose(request.status(), ChargeStatus.UNPAID), Instant.now());
    }

    public UtilityCharge update(Long id, UtilityChargeDto.Request request) {
            UtilityCharge old = getById(id);
        if (old.status() == ChargeStatus.PAID && (request.meterId() != null || request.period() != null
                || request.status() == ChargeStatus.UNPAID)) {
            throw ApiException.conflict("Оплаченное начисление нельзя пересчитать или вернуть в неоплаченные");
        }
        if (request.meterId() == null && request.period() == null) {
            return utilityChargeRepository.save(new UtilityCharge(id, old.premiseId(), old.meterId(),
                    old.period(), old.volume(), old.amount(), Checks.choose(request.status(), old.status()), old.createdAt()));
        }
        return calculate(id, Checks.choose(request.meterId(), old.meterId()),
                Checks.choose(request.period(), old.period()), Checks.choose(request.status(), old.status()), old.createdAt());
    }

    public void delete(Long id) {
            UtilityCharge old = getById(id);
        if (old.status() == ChargeStatus.PAID) {
            throw ApiException.conflict("Оплаченное начисление нельзя удалить");
        }
        utilityChargeRepository.deleteById(id);
    }

    private UtilityCharge calculate(Long id, Long meterId, YearMonth period, ChargeStatus status, Instant createdAt) {
        Checks.required(meterId, "meterId");
        Checks.required(period, "period");
        if (!period.isBefore(YearMonth.now())) throw ApiException.badRequest("Начисление возможно только за завершённый месяц");
        Meter meter = meterRepository.findById(meterId).orElseThrow(() -> ApiException.badRequest("Счётчик не существует"));
        for (UtilityCharge charge : utilityChargeRepository.findAll()) {
            if (!charge.id().equals(id) && charge.meterId().equals(meterId) && charge.period().equals(period)) {
                throw ApiException.conflict("Начисление за этот период уже существует");
            }
        }

        MeterReading current = null;
        MeterReading previousReading = null;
        for (MeterReading reading : meterReadingRepository.findAll()) {
            if (!reading.meterId().equals(meterId) || reading.status() != ReadingStatus.VERIFIED) {
                continue;
            }
            if (reading.period().equals(period)) {
                current = reading;
            }
            if (reading.period().isBefore(period)
                    && (previousReading == null || reading.period().isAfter(previousReading.period()))) {
                previousReading = reading;
            }
        }
        if (current == null) {
            throw ApiException.badRequest("Нет проверенного показания за этот период");
        }
        BigDecimal previous = BigDecimal.ZERO;
        if (previousReading != null) {
            previous = previousReading.value();
        }
        // Например: было 100, стало 115. Расход равен 15.
        BigDecimal volume = current.value().subtract(previous);
        if (volume.signum() < 0) throw ApiException.badRequest("Отрицательный расход");
        BigDecimal amount = volume.multiply(meter.tariff()).setScale(2, RoundingMode.HALF_UP);
        return utilityChargeRepository.save(new UtilityCharge(id, meter.premiseId(), meterId, period,
                volume, amount, status, createdAt));
    }
}
