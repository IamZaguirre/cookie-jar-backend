package com.cookiejar.repository;

import com.cookiejar.model.SiteAdvisory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteAdvisoryRepository extends JpaRepository<SiteAdvisory, Long> {
}