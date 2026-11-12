package com.ojt.p01.dto.response;

/**
 * DTO trả về thông tin bản ghi nhật ký hệ thống (Audit Log) cho Admin.
 */
public record AuditLogResponse(
        Long id,
        String timestamp,
        String createdAt,
        Long actorId,
        String actorName,
        String actor,
        String role,
        String action,
        String target,
        String targetType,
        String targetId,
        String result,
        String outcome,
        String srcIp,
        String correlationId,
        Integer httpStatus
) {}
