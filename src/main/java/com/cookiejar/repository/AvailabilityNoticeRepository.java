package com.cookiejar.repository;

import com.cookiejar.model.AvailabilityNotice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvailabilityNoticeRepository extends JpaRepository<AvailabilityNotice, Long> {
}
