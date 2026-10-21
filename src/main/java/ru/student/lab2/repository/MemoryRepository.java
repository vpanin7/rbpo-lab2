package ru.student.lab2.repository;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import ru.student.lab2.model.Identifiable;

public abstract class MemoryRepository<T extends Identifiable> {
    private final Map<Long, T> items = new ConcurrentSkipListMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public long nextId() { return sequence.incrementAndGet(); }
    public List<T> findAll() { return new ArrayList<>(items.values()); }
    public Optional<T> findById(Long id) { return Optional.ofNullable(items.get(id)); }
    public T save(T item) { items.put(item.id(), item); return item; }
    public void deleteById(Long id) { items.remove(id); }
}
