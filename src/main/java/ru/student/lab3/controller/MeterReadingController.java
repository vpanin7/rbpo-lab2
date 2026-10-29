package ru.student.lab3.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.student.lab3.dto.MeterReadingDto;
import ru.student.lab3.model.MeterReading;
import ru.student.lab3.service.MeterReadingService;

@RestController
@RequestMapping("/api/meter-readings")
public class MeterReadingController {
    private final MeterReadingService service;

    public MeterReadingController(MeterReadingService service) {
        this.service = service;
    }

    @GetMapping
    public List<MeterReadingDto.Response> getAll() {
        return service.getAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public MeterReadingDto.Response getById(@PathVariable Long id) {
        return toResponse(service.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MeterReadingDto.Response create(@RequestBody MeterReadingDto.Request request) {
        return toResponse(service.create(request));
    }

    @PatchMapping("/{id}")
    public MeterReadingDto.Response update(@PathVariable Long id, @RequestBody MeterReadingDto.Request request) {
        return toResponse(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    private MeterReadingDto.Response toResponse(MeterReading item) {
        return new MeterReadingDto.Response(item.id(), item.meterId(), item.period(), item.value(), item.status(), item.createdAt());
    }
}
