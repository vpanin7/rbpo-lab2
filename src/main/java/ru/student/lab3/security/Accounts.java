package ru.student.lab3.security;

import java.nio.charset.StandardCharsets;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.student.lab3.dto.UserDto;
import ru.student.lab3.exception.ApiException;
import ru.student.lab3.model.User;
import ru.student.lab3.service.UserService;

@Service
public class Accounts implements UserDetailsService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final UserService users;

    public Accounts(JdbcTemplate jdbc, PasswordEncoder encoder, UserService users) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return jdbc.query("""
                SELECT u.id, u.email, a.password_hash, a.role, a.enabled
                FROM app_users u JOIN user_accounts a ON a.user_id = u.id
                WHERE lower(u.email) = lower(?)
                """, (rs, row) -> new AccountPrincipal(rs.getLong("id"), rs.getString("email"),
                    rs.getString("password_hash"), rs.getString("role"), rs.getBoolean("enabled")),
                username.strip())
                .stream().findFirst().orElseThrow(() -> new UsernameNotFoundException("Учётная запись не найдена"));
    }

    @Transactional
    public User register(String name, String email, String password) {
        String hash = hashPassword(password);
        User user = users.create(new UserDto.Request(name, email));
        insert(user.id(), hash, "RESIDENT");
        return user;
    }

    public void setEnabled(long id, boolean enabled) {
        // Через API администратор управляет только обычными учётными записями.
        if (jdbc.update("UPDATE user_accounts SET enabled = ? WHERE user_id = ? AND role = 'RESIDENT'",
                enabled, id) == 0) {
            throw ApiException.notFound();
        }
    }

    @Transactional
    public void setupDemo(String adminPassword, String residentPassword) {
        String adminEmail = "admin@example.com";
        String adminHash = hashPassword(adminPassword);
        String residentHash = hashPassword(residentPassword);
        // Не повышаем права уже зарегистрированного пользователя.
        var roles = jdbc.queryForList("""
                SELECT a.role FROM app_users u LEFT JOIN user_accounts a ON u.id = a.user_id
                WHERE lower(u.email) = lower(?)
                """, String.class, adminEmail);
        if (roles.isEmpty()) {
            User admin = users.create(new UserDto.Request("Администратор", adminEmail));
            insert(admin.id(), adminHash, "ADMIN");
        } else if (!"ADMIN".equals(roles.get(0))) {
            throw new IllegalStateException("Адрес демонстрационного администратора уже занят");
        }

        for (String email : new String[]{"resident1@example.com", "resident2@example.com"}) {
            int count = jdbc.update("""
                    INSERT INTO user_accounts (user_id, password_hash, role)
                    SELECT id, ?, 'RESIDENT' FROM app_users WHERE lower(email) = ?
                    ON CONFLICT (user_id) DO NOTHING
                    """, residentHash, email);
            if (count == 0 && !Boolean.TRUE.equals(jdbc.queryForObject("""
                    SELECT EXISTS (SELECT 1 FROM app_users u JOIN user_accounts a ON a.user_id = u.id
                    WHERE lower(u.email) = lower(?) AND a.role = 'RESIDENT')
                    """, Boolean.class, email))) {
                throw new IllegalStateException("Сначала загрузите демонстрационные профили из db/seed.sql");
            }
        }
    }

    private void insert(long id, String hash, String role) {
        jdbc.update("INSERT INTO user_accounts (user_id, password_hash, role) VALUES (?, ?, ?)", id, hash, role);
    }

    private String hashPassword(String password) {
        if (password == null || password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72
                || !password.matches("(?s).*[A-Z].*") || !password.matches("(?s).*[a-z].*")
                || !password.matches("(?s).*[0-9].*") || !password.matches("(?s).*[^a-zA-Z0-9\\s].*")) {
            throw ApiException.badRequest("Пароль: от 12 символов, до 72 байт UTF-8; нужны A–Z, a–z, цифра и спецсимвол");
        }
        return encoder.encode(password);
    }
}
