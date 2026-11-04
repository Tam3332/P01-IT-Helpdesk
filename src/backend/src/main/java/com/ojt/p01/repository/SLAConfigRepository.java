package com.ojt.p01.repository;

import com.ojt.p01.entity.SLAConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SLAConfigRepository extends JpaRepository<SLAConfig, Integer> {

    /**
     * Tra cứu cấu hình SLA theo mức ưu tiên (BR-HD-03).
     * @param priorityLevel LOW | MEDIUM | HIGH | CRITICAL
     */
    Optional<SLAConfig> findByPriorityLevel(String priorityLevel);

    /**
     * Trả về toàn bộ cấu hình SLA theo thứ tự ưu tiên: CRITICAL → HIGH → MEDIUM → LOW.
     */
    @Query("SELECT s FROM SLAConfig s ORDER BY CASE s.priorityLevel " +
           "WHEN 'CRITICAL' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'MEDIUM' THEN 3 WHEN 'LOW' THEN 4 END")
    List<SLAConfig> findAllOrderByPriority();
}
