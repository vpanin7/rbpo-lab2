package ru.student.lab3.controller;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import ru.student.lab3.model.*;
import ru.student.lab3.service.UtilityOperationsService;

@RestController
@RequestMapping("/api/operations")
public class UtilityOperationsController {
    private final UtilityOperationsService service;
    public UtilityOperationsController(UtilityOperationsService service) { this.service = service; }
    public record PeriodRequest(YearMonth period) {}
    public record PaymentRequest(YearMonth period, Long ownerId, BigDecimal amount) {}
    public record TransferRequest(Long newOwnerId) {}
    public record ReplacementRequest(String serialNumber, BigDecimal tariff) {}

    @PostMapping("/meter-readings/{id}/verify")
    public MeterReading verify(@PathVariable Long id) { return service.verifyReading(id); }

    @PostMapping("/premises/{id}/bill")
    public List<UtilityCharge> bill(@PathVariable Long id, @RequestBody PeriodRequest request) {
        return service.billPremise(id, request.period());
    }

    @PostMapping("/premises/{id}/pay")
    public UtilityOperationsService.PaymentResult pay(@PathVariable Long id, @RequestBody PaymentRequest request) {
        return service.payPremise(id, request.period(), request.ownerId(), request.amount());
    }

    @PostMapping("/premises/{id}/transfer")
    public Premise transfer(@PathVariable Long id, @RequestBody TransferRequest request) {
        return service.transferOwnership(id, request.newOwnerId());
    }

    @PostMapping("/meters/{id}/replace")
    public UtilityOperationsService.ReplacementResult replace(@PathVariable Long id, @RequestBody ReplacementRequest request) {
        return service.replaceMeter(id, request.serialNumber(), request.tariff());
    }
}
