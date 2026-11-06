package ru.student.lab3.controller;

import java.math.BigDecimal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import ru.student.lab3.security.AccountPrincipal;
import java.time.YearMonth;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import ru.student.lab3.model.*;
import ru.student.lab3.service.UtilityOperationsService;

@RestController
@RequestMapping("/api/operations")
public class UtilityOperationsController {
    private final UtilityOperationsService service;
    public UtilityOperationsController(UtilityOperationsService service) { this.service = service; }
    public record PeriodRequest(YearMonth period) {}
    public record PaymentRequest(YearMonth period, BigDecimal amount) {}
    public record TransferRequest(Long newOwnerId) {}
    public record ReplacementRequest(String serialNumber, BigDecimal tariff) {}

    @PostMapping("/meter-readings/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public MeterReading verify(@PathVariable Long id) { return service.verifyReading(id); }

    @PostMapping("/premises/{id}/bill")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UtilityCharge> bill(@PathVariable Long id, @RequestBody PeriodRequest request) {
        return service.billPremise(id, request.period());
    }

    @PostMapping("/premises/{id}/pay")
    @PreAuthorize("hasRole('RESIDENT')")
    public UtilityOperationsService.PaymentResult pay(@PathVariable Long id, @RequestBody PaymentRequest request,
            @AuthenticationPrincipal AccountPrincipal user) {
        return service.payPremise(id, request.period(), user.getUserId(), request.amount());
    }

    @PostMapping("/premises/{id}/transfer")
    @PreAuthorize("hasRole('ADMIN')")
    public Premise transfer(@PathVariable Long id, @RequestBody TransferRequest request) {
        return service.transferOwnership(id, request.newOwnerId());
    }

    @PostMapping("/meters/{id}/replace")
    @PreAuthorize("hasRole('ADMIN')")
    public UtilityOperationsService.ReplacementResult replace(@PathVariable Long id, @RequestBody ReplacementRequest request) {
        return service.replaceMeter(id, request.serialNumber(), request.tariff());
    }
}
