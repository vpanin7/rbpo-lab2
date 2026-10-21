package ru.student.lab2.service;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import ru.student.lab2.dto.MeterReadingDto;
import ru.student.lab2.model.*;
import ru.student.lab2.repository.*;
import ru.student.lab2.exception.ApiException;

@Service
public class MeterReadingService {
    private final MeterReadingRepository meterReadingRepository;
    private final MeterRepository meterRepository;
    private final UtilityChargeRepository utilityChargeRepository;
    private final WriteLock lock;

    public MeterReadingService(MeterReadingRepository meterReadingRepository, MeterRepository meterRepository, UtilityChargeRepository utilityChargeRepository, WriteLock lock) {
        this.meterReadingRepository = meterReadingRepository;
        this.meterRepository = meterRepository;
        this.utilityChargeRepository = utilityChargeRepository;
        this.lock = lock;
    }

    public List<MeterReading> getAll() { return meterReadingRepository.findAll(); }
    public MeterReading getById(Long id) {
        return meterReadingRepository.findById(id).orElseThrow(ApiException::notFound);
    }

    public MeterReading create(MeterReadingDto.Request request) {
        synchronized (lock) {
            return save(new MeterReading(meterReadingRepository.nextId(), request.meterId(), request.period(), request.value(), request.status(), Instant.now()));
        }
    }

    public MeterReading update(Long id, MeterReadingDto.Request request) {
        synchronized (lock) {
            MeterReading old = getById(id);
            return save(new MeterReading(id, Checks.choose(request.meterId(), old.meterId()), Checks.choose(request.period(), old.period()), Checks.choose(request.value(), old.value()), Checks.choose(request.status(), old.status()), old.createdAt()));
        }
    }

    public void delete(Long id) {
        synchronized (lock) {
            MeterReading old = getById(id);
            if (utilityChargeRepository.findAll().stream().anyMatch(c -> c.meterId().equals(old.meterId()))) {
                throw ApiException.conflict("Сначала удалите связанные объекты");
            }
            meterReadingRepository.deleteById(id);
        }
    }

    private MeterReading save(MeterReading item) {
        Long meterId = Checks.required(item.meterId(), "meterId");
        Meter meter = meterRepository.findById(meterId).orElseThrow(() -> ApiException.badRequest("Счётчик не существует"));
        if (meter.status() != MeterStatus.ACTIVE) throw ApiException.conflict("Счётчик неактивен");
        YearMonth period = Checks.required(item.period(), "period");
        if (period.isAfter(YearMonth.now())) throw ApiException.badRequest("Период не может быть в будущем");
        Checks.nonNegative(item.value(), "value");
        MeterReading old = meterReadingRepository.findById(item.id()).orElse(null);
        if (utilityChargeRepository.findAll().stream().anyMatch(c -> (c.meterId().equals(meterId) && !period.isAfter(c.period()))
                || (old != null && c.meterId().equals(old.meterId())))) {
            throw ApiException.conflict("Перед изменением показаний удалите начисления этого счётчика");
        }
        for (MeterReading other : meterReadingRepository.findAll()) {
            if (other.id().equals(item.id()) || !other.meterId().equals(meterId)) continue;
            if (other.period().equals(period)) throw ApiException.conflict("Показание за этот период уже существует");
            if (other.status() == ReadingStatus.REJECTED || item.status() == ReadingStatus.REJECTED) continue;
            if ((other.period().isBefore(period) && other.value().compareTo(item.value()) > 0)
                    || (other.period().isAfter(period) && other.value().compareTo(item.value()) < 0)) {
                throw ApiException.badRequest("Накопительные показания не должны уменьшаться");
            }
        }
        return meterReadingRepository.save(new MeterReading(item.id(), meterId, period, item.value(),
                Checks.choose(item.status(), ReadingStatus.PENDING), item.createdAt()));
    }
}
