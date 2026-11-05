package com.ojt.p01.controller;

import com.ojt.p01.common.ApiResponse;
import com.ojt.p01.dto.request.CreateCommentRequest;
import com.ojt.p01.dto.response.CommentResponse;
import com.ojt.p01.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller quản lý bình luận và trao đổi trực tiếp trên ticket (HD08, UC-05).
 */
@Tag(name = "Comments", description = "Quản lý bình luận và ghi chú nội bộ trên ticket")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/tickets/{ticketId}/comments")
public class TicketCommentController {

    private final CommentService commentService;

    public TicketCommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * GET /api/v1/tickets/{ticketId}/comments (HD08, UC-05).
     * Employee chỉ thấy bình luận công khai (isInternal = false).
     * IT Staff, IT Manager, Admin thấy toàn bộ bao gồm ghi chú nội bộ.
     */
    @Operation(summary = "Lấy danh sách bình luận của ticket (HD08, UC-05)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
            @Parameter(description = "ID số hoặc mã ticket (HD-YYYY-XXXX)") @PathVariable String ticketId
    ) {
        return ResponseEntity.ok(ApiResponse.success(commentService.getComments(ticketId)));
    }

    /**
     * POST /api/v1/tickets/{ticketId}/comments (HD08, UC-05, IA-HD06).
     * Gửi bình luận công khai hoặc ghi chú nội bộ.
     */
    @Operation(summary = "Gửi bình luận mới hoặc ghi chú nội bộ (HD08, UC-05)")
    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @Parameter(description = "ID số hoặc mã ticket (HD-YYYY-XXXX)") @PathVariable String ticketId,
            @Valid @RequestBody CreateCommentRequest request,
            HttpServletRequest httpRequest
    ) {
        CommentResponse response = commentService.addComment(ticketId, request, httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Bình luận đã được ghi nhận", response));
    }
}
