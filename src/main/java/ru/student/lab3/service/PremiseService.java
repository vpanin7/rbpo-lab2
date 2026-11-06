package ru.student.lab3.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import ru.student.lab3.dto.PremiseDto;
import ru.student.lab3.model.*;
import ru.student.lab3.repository.*;
import ru.student.lab3.exception.ApiException;

@Service
// При ошибке все изменения операции откатываются.
// SERIALIZABLE не даёт параллельным запросам провести противоречивые изменения.
@Transactional(isolation = Isolation.SERIALIZABLE)
public class PremiseService {
    private final PremiseRepository premiseRepository;
    private final UserRepository userRepository;
    private final MeterRepository meterRepository;
    private final UtilityChargeRepository utilityChargeRepository;

    public PremiseService(PremiseRepository premiseRepository, UserRepository userRepository, MeterRepository meterRepository, UtilityChargeRepository utilityChargeRepository) {
        this.premiseRepository = premiseRepository;
        this.userRepository = userRepository;
        this.meterRepository = meterRepository;
        this.utilityChargeRepository = utilityChargeRepository;
    }

    public List<Premise> getAll() { return premiseRepository.findAll(); }
    public Premise getById(Long id) {
        return premiseRepository.findById(id).orElseThrow(ApiException::notFound);
    }

    public Premise create(PremiseDto.Request request) {
            return save(new Premise(premiseRepository.nextId(), request.address(), request.area(), request.ownerId(), Instant.now()));
    }

    public Premise update(Long id, PremiseDto.Request request) {
            Premise old = getById(id);
        return save(new Premise(id, Checks.choose(request.address(), old.address()), Checks.choose(request.area(), old.area()), Checks.choose(request.ownerId(), old.ownerId()), old.createdAt()));
    }

    public void delete(Long id) {
            Premise old = getById(id);
        if (meterRepository.findAll().stream().anyMatch(m -> m.premiseId().equals(id))
                || utilityChargeRepository.findAll().stream().anyMatch(c -> c.premiseId().equals(id))) {
            throw ApiException.conflict("Сначала удалите связанные объекты");
        }
        premiseRepository.deleteById(id);
    }

    private Premise save(Premise item) {
        String address = Checks.text(item.address(), "address");
        BigDecimal area = Checks.nonNegative(item.area(), "area");
        if (area.signum() == 0) throw ApiException.badRequest("Площадь должна быть больше нуля");
        Long ownerId = Checks.required(item.ownerId(), "ownerId");
        if (userRepository.findById(ownerId).isEmpty()) throw ApiException.badRequest("Владелец не существует");
        Premise old = premiseRepository.findById(item.id()).orElse(null);
        if (old != null && !old.ownerId().equals(ownerId) && utilityChargeRepository.findAll().stream()
                .anyMatch(c -> c.premiseId().equals(item.id()) && c.status() == ChargeStatus.UNPAID)) {
            throw ApiException.conflict("Нельзя сменить владельца при неоплаченных начислениях");
        }
        return premiseRepository.save(new Premise(item.id(), address, area, ownerId, item.createdAt()));
    }
}
