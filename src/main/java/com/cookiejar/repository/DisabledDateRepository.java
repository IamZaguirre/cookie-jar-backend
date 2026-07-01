package com.cookiejar.repository;

import com.cookiejar.model.DisabledDate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DisabledDateRepository extends JpaRepository<DisabledDate, Long> {
    Optional<DisabledDate> findByDate(LocalDate date);
    boolean existsByDate(LocalDate date);
    List<DisabledDate> findByDateBefore(LocalDate date);
    void deleteByDateBefore(LocalDate date);
}
