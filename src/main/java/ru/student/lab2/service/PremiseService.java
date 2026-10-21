package ru.student.lab2.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import ru.student.lab2.dto.PremiseDto;
import ru.student.lab2.model.*;
import ru.student.lab2.repository.*;
import ru.student.lab2.exception.ApiException;

@Service
public class PremiseService {
    private final PremiseRepository premiseRepository;
    private final UserRepository userRepository;
    private final MeterRepository meterRepository;
    private final UtilityChargeRepository utilityChargeRepository;
    private final WriteLock lock;

    public PremiseService(PremiseRepository premiseRepository, UserRepository userRepository, MeterRepository meterRepository, UtilityChargeRepository utilityChargeRepository, WriteLock lock) {
        this.premiseRepository = premiseRepository;
        this.userRepository = userRepository;
        this.meterRepository = meterRepository;
        this.utilityChargeRepository = utilityChargeRepository;
        this.lock = lock;
    }

    public List<Premise> getAll() { return premiseRepository.findAll(); }
    public Premise getById(Long id) {
        return premiseRepository.findById(id).orElseThrow(ApiException::notFound);
    }

    public Premise create(PremiseDto.Request request) {
        synchronized (lock) {
            return save(new Premise(premiseRepository.nextId(), request.address(), request.area(), request.ownerId(), Instant.now()));
        }
    }

    public Premise update(Long id, PremiseDto.Request request) {
        synchronized (lock) {
            Premise old = getById(id);
            return save(new Premise(id, Checks.choose(request.address(), old.address()), Checks.choose(request.area(), old.area()), Checks.choose(request.ownerId(), old.ownerId()), old.createdAt()));
        }
    }

    public void delete(Long id) {
        synchronized (lock) {
            Premise old = getById(id);
            if (meterRepository.findAll().stream().anyMatch(m -> m.premiseId().equals(id))
                    || utilityChargeRepository.findAll().stream().anyMatch(c -> c.premiseId().equals(id))) {
                throw ApiException.conflict("Сначала удалите связанные объекты");
            }
            premiseRepository.deleteById(id);
        }
    }

    private Premise save(Premise item) {
        String address = Checks.text(item.address(), "address");
        BigDecimal area = Checks.nonNegative(item.area(), "area");
        if (area.signum() == 0) throw ApiException.badRequest("Площадь должна быть больше нуля");
        Long ownerId = Checks.required(item.ownerId(), "ownerId");
        if (userRepository.findById(ownerId).isEmpty()) throw ApiException.badRequest("Владелец не существует");
        return premiseRepository.save(new Premise(item.id(), address, area, ownerId, item.createdAt()));
    }
}
