package ru.student.lab3.service;

import java.math.BigDecimal;
import ru.student.lab3.exception.ApiException;

final class Checks {
    private Checks() {}
    static <T> T required(T value, String field) {
        if (value == null) throw ApiException.badRequest("Обязательное поле: " + field);
        return value;
    }
    static String text(String value, String field) {
        required(value, field);
        if (value.isBlank()) throw ApiException.badRequest("Пустое поле: " + field);
        return value.trim();
    }
    static BigDecimal nonNegative(BigDecimal value, String field) {
        required(value, field);
        if (value.signum() < 0) throw ApiException.badRequest(field + " не может быть отрицательным");
        return value;
    }
    static <T> T choose(T value, T old) { return value == null ? old : value; }
}
