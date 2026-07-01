package com.cookiejar.service;

import com.cookiejar.repository.DisabledDateRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class DisabledDateCleanupService {

    private final DisabledDateRepository repository;

    public DisabledDateCleanupService(DisabledDateRepository repository) {
        this.repository = repository;
    }

    /** Runs every day at midnight and deletes any disabled dates that are in the past. */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void deletePastDisabledDates() {
        repository.deleteByDateBefore(LocalDate.now());
    }
}
