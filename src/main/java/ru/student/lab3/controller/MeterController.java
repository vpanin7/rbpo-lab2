package ru.student.lab3.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.student.lab3.dto.MeterDto;
import ru.student.lab3.model.Meter;
import ru.student.lab3.service.MeterService;

@RestController
@RequestMapping("/api/meters")
public class MeterController {
    private final MeterService service;

    public MeterController(MeterService service) {
        this.service = service;
    }

    @GetMapping
    public List<MeterDto.Response> getAll() {
        return service.getAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public MeterDto.Response getById(@PathVariable Long id) {
        return toResponse(service.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MeterDto.Response create(@RequestBody MeterDto.Request request) {
        return toResponse(service.create(request));
    }

    @PatchMapping("/{id}")
    public MeterDto.Response update(@PathVariable Long id, @RequestBody MeterDto.Request request) {
        return toResponse(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    private MeterDto.Response toResponse(Meter item) {
        return new MeterDto.Response(item.id(), item.premiseId(), item.resourceType(), item.serialNumber(), item.tariff(), item.status(), item.createdAt());
    }
}
