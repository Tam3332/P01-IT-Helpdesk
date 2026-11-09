package com.ojt.p01.dto.response;

import java.time.OffsetDateTime;

/**
 * ReopenRequestDTO (openapi.yaml, SDD §4).
 * ticketTitle, previousResolutionNote giúp IT Manager đối chiếu lý do với giải pháp cũ
 * (BPMN P3 "Review the Reopen Reason & Compare Against the Previous Resolution").
 */
public record ReopenRequestResponse(
        Long id,
        Long ticketId,
        String ticketNumber,
        String ticketTitle,
        Long requesterId,
        String requesterName,
        String reopenReason,
        String previousResolutionNote,
        String status,
        Long reviewedBy,
        String reviewNote,
        OffsetDateTime createdAt,
        OffsetDateTime reviewedAt
) {}
