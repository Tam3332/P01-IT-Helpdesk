package com.ojt.p01.repository;

import com.ojt.p01.entity.SLAConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SLAConfigRepository extends JpaRepository<SLAConfig, Integer> {

    /**
     * Tra cứu cấu hình SLA theo mức ưu tiên (BR-HD-03).
     * @param priorityLevel LOW | MEDIUM | HIGH | CRITICAL
     */
    Optional<SLAConfig> findByPriorityLevel(String priorityLevel);
}
