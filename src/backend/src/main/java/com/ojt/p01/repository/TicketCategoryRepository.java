package com.ojt.p01.repository;

import com.ojt.p01.entity.TicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketCategoryRepository extends JpaRepository<TicketCategory, Integer> {

    /** Lấy danh mục đang hoạt động, sắp xếp A-Z theo tên. */
    List<TicketCategory> findByIsActiveTrueOrderByCategoryNameAsc();
}
