package ru.student.lab3.dto;

import java.time.Instant;
import ru.student.lab3.model.*;

public class UserDto {
    public record Request(String name, String email) {}
    public record Response(Long id, String name, String email, Instant createdAt) {}
}
