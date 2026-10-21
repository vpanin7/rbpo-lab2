package ru.student.lab2.repository;

import org.springframework.stereotype.Repository;
import ru.student.lab2.model.User;

@Repository
public class UserRepository extends MemoryRepository<User> {
}
