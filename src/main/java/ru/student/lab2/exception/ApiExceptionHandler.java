package ru.student.lab2.exception;

import java.time.Instant;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    public record ErrorResponse(Instant timestamp, int status, String message) {}

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handle(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(
                new ErrorResponse(Instant.now(), ex.getStatus().value(), ex.getMessage()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> badInput(Exception ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(Instant.now(), 400,
                "Некорректный запрос: проверьте поля, типы данных и формат периода YYYY-MM"));
    }
}
