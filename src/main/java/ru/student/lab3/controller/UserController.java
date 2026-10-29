package ru.student.lab3.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.student.lab3.dto.UserDto;
import ru.student.lab3.model.User;
import ru.student.lab3.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public List<UserDto.Response> getAll() {
        return service.getAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public UserDto.Response getById(@PathVariable Long id) {
        return toResponse(service.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto.Response create(@RequestBody UserDto.Request request) {
        return toResponse(service.create(request));
    }

    @PatchMapping("/{id}")
    public UserDto.Response update(@PathVariable Long id, @RequestBody UserDto.Request request) {
        return toResponse(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    private UserDto.Response toResponse(User item) {
        return new UserDto.Response(item.id(), item.name(), item.email(), item.createdAt());
    }
}
