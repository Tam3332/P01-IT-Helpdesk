package com.ojt.p01.controller;

import com.ojt.p01.common.ApiResponse;
import com.ojt.p01.dto.response.AttachmentResponse;
import com.ojt.p01.entity.TicketAttachment;
import com.ojt.p01.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Controller quản lý tệp đính kèm an toàn trên ticket.
 */
@Tag(name = "Attachments", description = "Quản lý tệp đính kèm và tải xuống an toàn")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/tickets/{ticketId}/attachments")
public class TicketAttachmentController {

    private final AttachmentService attachmentService;

    public TicketAttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

 /**
 * GET /api/v1/tickets/{ticketId}/attachments.
 */
    @Operation(summary = "Lấy danh sách tệp đính kèm của ticket")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AttachmentResponse>>> getAttachments(
            @Parameter(description = "ID số hoặc mã ticket (HD-YYYY-XXXX)") @PathVariable String ticketId
    ) {
        return ResponseEntity.ok(ApiResponse.success(attachmentService.getAttachments(ticketId)));
    }

 /**
 * POST /api/v1/tickets/{ticketId}/attachments.
 */
    @Operation(summary = "Tải tệp đính kèm lên ticket")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AttachmentResponse>> uploadAttachment(
            @Parameter(description = "ID số hoặc mã ticket (HD-YYYY-XXXX)") @PathVariable String ticketId,
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "ID bình luận để gắn tệp (tùy chọn, phải thuộc cùng ticket)")
            @RequestParam(value = "commentId", required = false) Long commentId,
            HttpServletRequest httpRequest
    ) {
        AttachmentResponse response = attachmentService.uploadAttachment(ticketId, file, commentId, httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tải tệp lên thành công", response));
    }

 /**
 * GET /api/v1/tickets/{ticketId}/attachments/{attachmentId}.
 */
    @Operation(summary = "Tải xuống tệp tin đính kèm an toàn")
    @GetMapping("/{attachmentId}")
    public ResponseEntity<Resource> downloadAttachment(
            @Parameter(description = "ID số hoặc mã ticket (HD-YYYY-XXXX)") @PathVariable String ticketId,
            @PathVariable Long attachmentId
    ) {
        TicketAttachment attachment = attachmentService.getAttachmentEntity(ticketId, attachmentId);
        Resource resource = attachmentService.loadAttachmentFile(ticketId, attachmentId);

        // Bỏ dấu ngoặc kép, \\, xuống dòng để tên tệp không phá cấu trúc header (dữ liệu cũ có thể chưa làm sạch)
        String safeName = attachment.getFileName().replaceAll("[\"\\\\\\r\\n]", "_");
        String encodedFilename = URLEncoder.encode(safeName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeName + "\"; filename*=UTF-8''" + encodedFilename)
                .header("X-Content-Type-Options", "nosniff") // Bảo mật MIME confusion
                .body(resource);
    }
}
