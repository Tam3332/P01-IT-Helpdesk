package com.ojt.p01.controller;

import com.ojt.p01.common.ApiResponse;
import com.ojt.p01.dto.request.CreateFeedbackRequest;
import com.ojt.p01.dto.response.FeedbackResponse;
import com.ojt.p01.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tickets/{idOrNumber}/feedback")
@Tag(name = "Ticket Feedback", description = "API đánh giá chất lượng hỗ trợ sau khi đóng ticket (CSAT, )")
public class TicketFeedbackController {

    private final FeedbackService feedbackService;

    public TicketFeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @Operation(summary = "Lấy thông tin đánh giá của ticket")
    @GetMapping
    public ResponseEntity<ApiResponse<FeedbackResponse>> getFeedback(
            @PathVariable String idOrNumber) {
        FeedbackResponse response = feedbackService.getFeedback(idOrNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Gửi đánh giá chất lượng hỗ trợ (1-5 sao kèm nhận xét)")
    @PostMapping
    public ResponseEntity<ApiResponse<FeedbackResponse>> createFeedback(
            @PathVariable String idOrNumber,
            @Valid @RequestBody CreateFeedbackRequest request,
            HttpServletRequest servletRequest) {
        String clientIp = servletRequest.getRemoteAddr();
        FeedbackResponse response = feedbackService.createFeedback(idOrNumber, request, clientIp);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đánh giá chất lượng hỗ trợ thành công", response));
    }
}
