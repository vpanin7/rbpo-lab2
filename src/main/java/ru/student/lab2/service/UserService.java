package ru.student.lab2.service;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import ru.student.lab2.dto.UserDto;
import ru.student.lab2.model.*;
import ru.student.lab2.repository.*;
import ru.student.lab2.exception.ApiException;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PremiseRepository premiseRepository;
    private final WriteLock lock;

    public UserService(UserRepository userRepository, PremiseRepository premiseRepository, WriteLock lock) {
        this.userRepository = userRepository;
        this.premiseRepository = premiseRepository;
        this.lock = lock;
    }

    public List<User> getAll() { return userRepository.findAll(); }
    public User getById(Long id) {
        return userRepository.findById(id).orElseThrow(ApiException::notFound);
    }

    public User create(UserDto.Request request) {
        synchronized (lock) {
            return save(new User(userRepository.nextId(), request.name(), request.email(), Instant.now()));
        }
    }

    public User update(Long id, UserDto.Request request) {
        synchronized (lock) {
            User old = getById(id);
            return save(new User(id, Checks.choose(request.name(), old.name()), Checks.choose(request.email(), old.email()), old.createdAt()));
        }
    }

    public void delete(Long id) {
        synchronized (lock) {
            User old = getById(id);
            if (premiseRepository.findAll().stream().anyMatch(p -> p.ownerId().equals(id))) {
                throw ApiException.conflict("Сначала удалите связанные объекты");
            }
            userRepository.deleteById(id);
        }
    }

    private User save(User item) {
        String name = Checks.text(item.name(), "name");
        String email = Checks.text(item.email(), "email");
        if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw ApiException.badRequest("Некорректный email");
        if (userRepository.findAll().stream().anyMatch(u -> !u.id().equals(item.id()) && u.email().equalsIgnoreCase(email))) {
            throw ApiException.conflict("Пользователь с таким email уже существует");
        }
        return userRepository.save(new User(item.id(), name, email, item.createdAt()));
    }
}
