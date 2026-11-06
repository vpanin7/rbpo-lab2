package ru.student.lab3.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import ru.student.lab3.dto.UserDto;
import ru.student.lab3.security.AccountPrincipal;
import ru.student.lab3.security.Accounts;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final Accounts accounts;

    public AuthController(Accounts accounts) { this.accounts = accounts; }

    public record Registration(String name, String email, String password) {
        @Override public String toString() { return "Registration[password=hidden]"; }
    }
    public record Token(String headerName, String token) {}
    public record Profile(long userId, String email, String role) {}
    public record AccountState(Boolean enabled) {}

    @GetMapping("/csrf")
    public ResponseEntity<Token> csrf(CsrfToken token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new Token(token.getHeaderName(), token.getToken()));
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto.Response register(@RequestBody Registration form) {
        var user = accounts.register(form.name(), form.email(), form.password());
        return new UserDto.Response(user.id(), user.name(), user.email(), user.createdAt());
    }

    @GetMapping("/auth/me")
    public Profile me(@AuthenticationPrincipal AccountPrincipal user) {
        return new Profile(user.getUserId(), user.getUsername(), user.getRole());
    }

    @PatchMapping("/users/{id}/account")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void account(@PathVariable long id, @RequestBody AccountState state) {
        if (state.enabled() == null) {
            throw ru.student.lab3.exception.ApiException.badRequest("Укажите enabled");
        }
        accounts.setEnabled(id, state.enabled());
    }
}
