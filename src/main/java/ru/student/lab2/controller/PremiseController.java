package ru.student.lab2.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.student.lab2.dto.PremiseDto;
import ru.student.lab2.model.Premise;
import ru.student.lab2.service.PremiseService;

@RestController
@RequestMapping("/api/premises")
public class PremiseController {
    private final PremiseService service;

    public PremiseController(PremiseService service) {
        this.service = service;
    }

    @GetMapping
    public List<PremiseDto.Response> getAll() {
        return service.getAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public PremiseDto.Response getById(@PathVariable Long id) {
        return toResponse(service.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PremiseDto.Response create(@RequestBody PremiseDto.Request request) {
        return toResponse(service.create(request));
    }

    @PatchMapping("/{id}")
    public PremiseDto.Response update(@PathVariable Long id, @RequestBody PremiseDto.Request request) {
        return toResponse(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    private PremiseDto.Response toResponse(Premise item) {
        return new PremiseDto.Response(item.id(), item.address(), item.area(), item.ownerId(), item.createdAt());
    }
}
