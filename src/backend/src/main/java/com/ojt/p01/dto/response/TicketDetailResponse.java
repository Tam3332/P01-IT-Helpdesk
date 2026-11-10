package com.ojt.p01.dto.response;

import java.time.OffsetDateTime;

/**
 * DTO trả về chi tiết đầy đủ của một Ticket (GET /api/v1/tickets/{ticketNumber}).
 */
public record TicketDetailResponse(
        Long id,
        String ticketNumber,
        String title,
        String description,
        
        // Category
        Integer categoryId,
        String categoryCode,
        String categoryName,
        
        // Status & Priority
        String impactLevel,
        String priority,
        String status,
        
        // Requester (Người tạo)
        Long requesterId,
        String requesterName,
        String requesterEmail,
        String requesterDepartment,
        
        // Assignee (Kỹ thuật viên đang xử lý)
        Long assigneeId,
        String assigneeName,
        String assigneeEmail,
        
        // SLA Deadlines
        OffsetDateTime responseDeadline,
        OffsetDateTime resolutionDeadline,
        Boolean isSlaBreached,
        
        // Timestamps
        OffsetDateTime firstRespondedAt,
        OffsetDateTime resolvedAt,
        OffsetDateTime closedAt,
        OffsetDateTime reopenedAt,
        
        // Reopen info & Resolution
        Integer reopenCount,
        String reopenReason,
        String resolutionNote,
        
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
