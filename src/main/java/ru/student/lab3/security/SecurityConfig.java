package ru.student.lab3.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import ru.student.lab3.exception.ApiExceptionHandler.ErrorResponse;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http, ObjectMapper json) throws Exception {
        AuthenticationEntryPoint unauthorized = (request, response, error) -> {
            response.setHeader("WWW-Authenticate", "Basic realm=\"Utilities\"");
            writeError(json, response, 401, "Не удалось выполнить вход");
        };
        AccessDeniedHandler forbidden = (request, response, error) -> writeError(json, response, 403,
                error instanceof CsrfException ? "Отсутствует или неверен CSRF-токен" : "Недостаточно прав");

        return http
                // Basic проверяется в каждом запросе; сессия используется только для CSRF.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(c -> c.disable())
                // Basic не создаёт сессию входа, поэтому не сбрасываем CSRF после каждой проверки пароля.
                .csrf(c -> c.csrfTokenRepository(new HttpSessionCsrfTokenRepository())
                        .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy()))
                .httpBasic(b -> b.authenticationEntryPoint(unauthorized))
                .logout(l -> l.disable())
                .exceptionHandling(e -> e.authenticationEntryPoint(unauthorized).accessDeniedHandler(forbidden))
                .authorizeHttpRequests(a -> a
                        .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me", "/api/premises/{id}").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/operations/premises/{id}/pay").hasRole("RESIDENT")
                        .requestMatchers("/api/**").hasRole("ADMIN")
                        .anyRequest().denyAll())
                .build();
    }

    private void writeError(ObjectMapper json, HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        json.writeValue(response.getOutputStream(), new ErrorResponse(Instant.now(), status, message));
    }
}
