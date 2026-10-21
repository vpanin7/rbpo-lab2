package ru.student.lab2.service;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import ru.student.lab2.dto.MeterDto;
import ru.student.lab2.model.*;
import ru.student.lab2.repository.*;
import ru.student.lab2.exception.ApiException;

@Service
public class MeterService {
    private final MeterRepository meterRepository;
    private final PremiseRepository premiseRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final UtilityChargeRepository utilityChargeRepository;
    private final WriteLock lock;

    public MeterService(MeterRepository meterRepository, PremiseRepository premiseRepository, MeterReadingRepository meterReadingRepository, UtilityChargeRepository utilityChargeRepository, WriteLock lock) {
        this.meterRepository = meterRepository;
        this.premiseRepository = premiseRepository;
        this.meterReadingRepository = meterReadingRepository;
        this.utilityChargeRepository = utilityChargeRepository;
        this.lock = lock;
    }

    public List<Meter> getAll() { return meterRepository.findAll(); }
    public Meter getById(Long id) {
        return meterRepository.findById(id).orElseThrow(ApiException::notFound);
    }

    public Meter create(MeterDto.Request request) {
        synchronized (lock) {
            return save(new Meter(meterRepository.nextId(), request.premiseId(), request.resourceType(), request.serialNumber(), request.tariff(), request.status(), Instant.now()));
        }
    }

    public Meter update(Long id, MeterDto.Request request) {
        synchronized (lock) {
            Meter old = getById(id);
            return save(new Meter(id, Checks.choose(request.premiseId(), old.premiseId()), Checks.choose(request.resourceType(), old.resourceType()), Checks.choose(request.serialNumber(), old.serialNumber()), Checks.choose(request.tariff(), old.tariff()), Checks.choose(request.status(), old.status()), old.createdAt()));
        }
    }

    public void delete(Long id) {
        synchronized (lock) {
            Meter old = getById(id);
            if (meterReadingRepository.findAll().stream().anyMatch(r -> r.meterId().equals(id))
                    || utilityChargeRepository.findAll().stream().anyMatch(c -> c.meterId().equals(id))) {
                throw ApiException.conflict("Сначала удалите связанные объекты");
            }
            meterRepository.deleteById(id);
        }
    }

    private Meter save(Meter item) {
        Long premiseId = Checks.required(item.premiseId(), "premiseId");
        if (premiseRepository.findById(premiseId).isEmpty()) throw ApiException.badRequest("Помещение не существует");
        Checks.required(item.resourceType(), "resourceType");
        String serial = Checks.text(item.serialNumber(), "serialNumber");
        Checks.nonNegative(item.tariff(), "tariff");
        if (meterRepository.findAll().stream().anyMatch(m -> !m.id().equals(item.id()) && m.serialNumber().equals(serial))) {
            throw ApiException.conflict("Серийный номер уже используется");
        }
        Meter old = meterRepository.findById(item.id()).orElse(null);
        if (old != null && (!old.premiseId().equals(premiseId) || old.resourceType() != item.resourceType())
                && (meterReadingRepository.findAll().stream().anyMatch(r -> r.meterId().equals(item.id()))
                || utilityChargeRepository.findAll().stream().anyMatch(c -> c.meterId().equals(item.id())))) {
            throw ApiException.conflict("Нельзя менять помещение и ресурс счётчика с показаниями или начислениями");
        }
        return meterRepository.save(new Meter(item.id(), premiseId, item.resourceType(), serial,
                item.tariff(), Checks.choose(item.status(), MeterStatus.ACTIVE), item.createdAt()));
    }
}
