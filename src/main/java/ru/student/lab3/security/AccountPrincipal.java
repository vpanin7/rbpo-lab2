package ru.student.lab3.security;

import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class AccountPrincipal extends User {
    private final long userId;
    private final String role;

    public AccountPrincipal(long userId, String email, String hash, String role, boolean enabled) {
        super(email, hash, enabled, true, true, true, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        this.userId = userId;
        this.role = role;
    }

    public long getUserId() { return userId; }
    public String getRole() { return role; }
}
