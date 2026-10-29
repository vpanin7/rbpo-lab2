package ru.student.lab3.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.student.lab3.dto.UtilityChargeDto;
import ru.student.lab3.model.UtilityCharge;
import ru.student.lab3.service.UtilityChargeService;

@RestController
@RequestMapping("/api/utility-charges")
public class UtilityChargeController {
    private final UtilityChargeService service;

    public UtilityChargeController(UtilityChargeService service) {
        this.service = service;
    }

    @GetMapping
    public List<UtilityChargeDto.Response> getAll() {
        return service.getAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public UtilityChargeDto.Response getById(@PathVariable Long id) {
        return toResponse(service.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UtilityChargeDto.Response create(@RequestBody UtilityChargeDto.Request request) {
        return toResponse(service.create(request));
    }

    @PatchMapping("/{id}")
    public UtilityChargeDto.Response update(@PathVariable Long id, @RequestBody UtilityChargeDto.Request request) {
        return toResponse(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    private UtilityChargeDto.Response toResponse(UtilityCharge item) {
        return new UtilityChargeDto.Response(item.id(), item.premiseId(), item.meterId(), item.period(), item.volume(), item.amount(), item.status(), item.createdAt());
    }
}
