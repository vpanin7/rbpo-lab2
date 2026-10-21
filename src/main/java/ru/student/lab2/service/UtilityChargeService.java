package ru.student.lab2.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import ru.student.lab2.dto.UtilityChargeDto;
import ru.student.lab2.model.*;
import ru.student.lab2.repository.*;
import ru.student.lab2.exception.ApiException;

@Service
public class UtilityChargeService {
    private final UtilityChargeRepository utilityChargeRepository;
    private final MeterRepository meterRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final WriteLock lock;

    public UtilityChargeService(UtilityChargeRepository utilityChargeRepository, MeterRepository meterRepository, MeterReadingRepository meterReadingRepository, WriteLock lock) {
        this.utilityChargeRepository = utilityChargeRepository;
        this.meterRepository = meterRepository;
        this.meterReadingRepository = meterReadingRepository;
        this.lock = lock;
    }

    public List<UtilityCharge> getAll() { return utilityChargeRepository.findAll(); }
    public UtilityCharge getById(Long id) {
        return utilityChargeRepository.findById(id).orElseThrow(ApiException::notFound);
    }

    public UtilityCharge create(UtilityChargeDto.Request request) {
        synchronized (lock) {
            return calculate(utilityChargeRepository.nextId(), request.meterId(), request.period(),
                    Checks.choose(request.status(), ChargeStatus.UNPAID), Instant.now());
        }
    }

    public UtilityCharge update(Long id, UtilityChargeDto.Request request) {
        synchronized (lock) {
            UtilityCharge old = getById(id);
            if (request.meterId() == null && request.period() == null) {
                return utilityChargeRepository.save(new UtilityCharge(id, old.premiseId(), old.meterId(),
                        old.period(), old.volume(), old.amount(), Checks.choose(request.status(), old.status()), old.createdAt()));
            }
            return calculate(id, Checks.choose(request.meterId(), old.meterId()),
                    Checks.choose(request.period(), old.period()), Checks.choose(request.status(), old.status()), old.createdAt());
        }
    }

    public void delete(Long id) {
        synchronized (lock) {
            UtilityCharge old = getById(id);
            if (false) {
                throw ApiException.conflict("Сначала удалите связанные объекты");
            }
            utilityChargeRepository.deleteById(id);
        }
    }

    private UtilityCharge calculate(Long id, Long meterId, YearMonth period, ChargeStatus status, Instant createdAt) {
        Checks.required(meterId, "meterId");
        Checks.required(period, "period");
        Meter meter = meterRepository.findById(meterId).orElseThrow(() -> ApiException.badRequest("Счётчик не существует"));
        if (utilityChargeRepository.findAll().stream().anyMatch(c -> !c.id().equals(id)
                && c.meterId().equals(meterId) && c.period().equals(period))) {
            throw ApiException.conflict("Начисление за этот период уже существует");
        }
        List<MeterReading> readings = meterReadingRepository.findAll().stream()
                .filter(r -> r.meterId().equals(meterId) && r.status() == ReadingStatus.VERIFIED).toList();
        MeterReading current = readings.stream().filter(r -> r.period().equals(period)).findFirst()
                .orElseThrow(() -> ApiException.badRequest("Нет проверенного показания за этот период"));
        BigDecimal previous = readings.stream().filter(r -> r.period().isBefore(period))
                .max(java.util.Comparator.comparing(MeterReading::period)).map(MeterReading::value).orElse(BigDecimal.ZERO);
        BigDecimal volume = current.value().subtract(previous);
        if (volume.signum() < 0) throw ApiException.badRequest("Отрицательный расход");
        BigDecimal amount = volume.multiply(meter.tariff()).setScale(2, RoundingMode.HALF_UP);
        return utilityChargeRepository.save(new UtilityCharge(id, meter.premiseId(), meterId, period,
                volume, amount, status, createdAt));
    }
}
