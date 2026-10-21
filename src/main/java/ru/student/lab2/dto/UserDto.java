package ru.student.lab2.dto;

import java.time.Instant;
import ru.student.lab2.model.*;

public class UserDto {
    public record Request(String name, String email) {}
    public record Response(Long id, String name, String email, Instant createdAt) {}
}
