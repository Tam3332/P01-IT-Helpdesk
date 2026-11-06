package com.ojt.p01.service;

import com.ojt.p01.dto.response.AttachmentResponse;
import com.ojt.p01.audit.AuditLogger;
import com.ojt.p01.entity.Ticket;
import com.ojt.p01.entity.TicketAttachment;
import com.ojt.p01.entity.TicketHistory;
import com.ojt.p01.entity.User;
import com.ojt.p01.exception.AppException;
import com.ojt.p01.exception.ErrorCode;
import com.ojt.p01.repository.TicketAttachmentRepository;
import com.ojt.p01.repository.TicketHistoryRepository;
import com.ojt.p01.repository.TicketRepository;
import com.ojt.p01.repository.UserRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service xử lý tệp đính kèm an toàn (HD04, IA-HD03).
 */
@Service
public class AttachmentService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB (IA-HD03)
    private static final long MAX_FILES_PER_TICKET = 5;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "pdf", "docx", "xlsx", "txt", "log"
    );

    // MIME đúng cho từng đuôi file (SRS §4 HD04 / SecRS IA-HD03: kiểm tra MIME type). Lưu DB theo bảng này, không tin giá trị client gửi.
    private static final java.util.Map<String, String> EXPECTED_MIME = java.util.Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "gif", "image/gif",
            "pdf", "application/pdf",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "txt", "text/plain",
            "log", "text/plain"
    );

    private final Path uploadRoot = Paths.get("uploads").toAbsolutePath().normalize();

    private final TicketAttachmentRepository attachmentRepository;
    private final TicketRepository ticketRepository;
    private final TicketHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final AuditLogger auditLogger;

    public AttachmentService(TicketAttachmentRepository attachmentRepository,
                             TicketRepository ticketRepository,
                             TicketHistoryRepository historyRepository,
                             UserRepository userRepository,
                             AuditLogger auditLogger) {
        this.attachmentRepository = attachmentRepository;
        this.ticketRepository = ticketRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.auditLogger = auditLogger;

        try {
            Files.createDirectories(this.uploadRoot);
        } catch (IOException e) {
            throw new RuntimeException("Không thể khởi tạo thư mục lưu trữ uploads", e);
        }
    }

    /**
     * Lấy danh sách tệp đính kèm (GET /api/v1/tickets/{id}/attachments).
     */
    @Transactional(readOnly = true)
    public List<AttachmentResponse> getAttachments(String idOrNumber) {
        Ticket ticket = findTicket(idOrNumber);
        validateAccess(ticket);

        return attachmentRepository.findByTicket_IdOrderByCreatedAtAsc(ticket.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Tải tệp lên ticket (POST /api/v1/tickets/{id}/attachments).
     * Kiểm tra dung lượng <= 10MB, tối đa 5 file/ticket, định dạng whitelist, hash SHA-256 (IA-HD03).
     */
    @Transactional
    public AttachmentResponse uploadAttachment(String idOrNumber, MultipartFile file, String ipAddress) {
        Ticket ticket = findTicket(idOrNumber);
        validateAccess(ticket);

        if ("CLOSED".equalsIgnoreCase(ticket.getStatus())) {
            throw new AppException(ErrorCode.TICKET_CLOSED);
        }

        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Tệp tải lên không được để trống");
        }

        // 1. Kiểm tra dung lượng <= 10MB
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE);
        }

        // 2. Kiểm tra số lượng tệp tối đa 5 tệp / ticket
        long currentCount = attachmentRepository.countByTicket_Id(ticket.getId());
        if (currentCount >= MAX_FILES_PER_TICKET) {
            throw new AppException(ErrorCode.ATTACHMENT_LIMIT_EXCEEDED);
        }

        // 3. Kiểm tra phần mở rộng file (Extension Whitelist)
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "file_" + System.currentTimeMillis();
        }
        String ext = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(ext.toLowerCase())) {
            throw new AppException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
        String extLower = ext.toLowerCase(java.util.Locale.ROOT);
        String expectedMime = EXPECTED_MIME.get(extLower);

        // 3b. MIME client khai báo phải khớp đuôi file (SecRS IA-HD03).
        // Bỏ qua khi client không gửi hoặc gửi chung chung application/octet-stream; tệp văn bản chấp nhận mọi text/*.
        String declaredMime = file.getContentType();
        if (declaredMime != null && !declaredMime.isBlank()
                && !"application/octet-stream".equalsIgnoreCase(declaredMime)) {
            String declaredLower = declaredMime.toLowerCase(java.util.Locale.ROOT);
            boolean mimeOk = expectedMime.startsWith("text/")
                    ? declaredLower.startsWith("text/")
                    : declaredLower.startsWith(expectedMime);
            if (!mimeOk) {
                throw new AppException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
            }
        }

        // 3c. Chữ ký nội dung (magic bytes) phải đúng loại tệp, chống đổi đuôi file (SecRS IA-HD03)
        if (!hasValidSignature(file, extLower)) {
            throw new AppException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }

        // 4. Lưu trữ tệp ra đĩa và tính mã băm SHA-256
        User currentUser = getCurrentUser();
        String storedFileName = UUID.randomUUID() + "_" + sanitizeFileName(originalFilename);
        Path targetDir = uploadRoot.resolve(String.valueOf(ticket.getId()));
        Path targetPath = targetDir.resolve(storedFileName);

        String sha256;
        try {
            Files.createDirectories(targetDir);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            try (InputStream is = file.getInputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            sha256 = bytesToHex(digest.digest());

            // Lưu file
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Lỗi trong quá trình lưu trữ tệp: " + e.getMessage());
        }

        // Lưu MIME do server xác định theo đuôi file đã kiểm tra, không lưu giá trị client gửi
        String mimeType = expectedMime;
        TicketAttachment attachment = new TicketAttachment(
                ticket,
                null,
                currentUser,
                originalFilename,
                targetPath.toString(),
                file.getSize(),
                mimeType,
                sha256
        );

        TicketAttachment saved = attachmentRepository.save(attachment);

        // Ghi nhận vào TicketHistory (HD09)
        String clientIp = (ipAddress != null && !ipAddress.isBlank()) ? ipAddress : "127.0.0.1";
        historyRepository.save(new TicketHistory(
                ticket,
                currentUser,
                "ATTACH_FILE",
                "attachment_id",
                null,
                String.valueOf(saved.getId()),
                "Đính kèm tệp: " + originalFilename + " (" + (file.getSize() / 1024) + " KB)",
                clientIp,
                saved.getCreatedAt()
        ));

        return toResponse(saved);
    }

    /**
     * Tải xuống tệp đính kèm an toàn (GET /api/v1/tickets/{id}/attachments/{attachmentId}).
     */
    @Transactional(readOnly = true)
    public Resource loadAttachmentFile(String idOrNumber, Long attachmentId) {
        Ticket ticket = findTicket(idOrNumber);
        validateAccess(ticket);

        TicketAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new AppException(ErrorCode.ATTACHMENT_NOT_FOUND));

        if (!attachment.getTicket().getId().equals(ticket.getId())) {
            throw new AppException(ErrorCode.ATTACHMENT_NOT_FOUND);
        }

        try {
            Path filePath = Paths.get(attachment.getStoragePath());
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new AppException(ErrorCode.ATTACHMENT_NOT_FOUND, "Không thể đọc tệp tin từ máy chủ");
            }
        } catch (MalformedURLException e) {
            throw new AppException(ErrorCode.ATTACHMENT_NOT_FOUND, "Đường dẫn tệp tin không hợp lệ");
        }
    }

    @Transactional(readOnly = true)
    public TicketAttachment getAttachmentEntity(String idOrNumber, Long attachmentId) {
        Ticket ticket = findTicket(idOrNumber);
        validateAccess(ticket);

        TicketAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new AppException(ErrorCode.ATTACHMENT_NOT_FOUND));

        if (!attachment.getTicket().getId().equals(ticket.getId())) {
            throw new AppException(ErrorCode.ATTACHMENT_NOT_FOUND);
        }

        return attachment;
    }

    private void validateAccess(Ticket ticket) {
        User currentUser = getCurrentUser();
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "EMPLOYEE";

        if ("EMPLOYEE".equalsIgnoreCase(roleName)) {
            if (!ticket.getRequester().getId().equals(currentUser.getId())) {
                // Trái quyền → 404 như vé không tồn tại + audit AUTHZ_DENIED (SDD §7)
                auditLogger.ticketDenied(currentUser, ticket.getTicketNumber(), "ACCESS_ATTACHMENT", 404);
                throw new AppException(ErrorCode.TICKET_NOT_FOUND);
            }
        } else if ("IT_STAFF".equalsIgnoreCase(roleName)) {
            // IT_STAFF chỉ thao tác trên vé được giao cho mình (SRS §4 HD05 – bản sửa 05/11; SecRS §5 POST attachments: "Assigned only")
            boolean isAssignedToMe = ticket.getCurrentAssignee() != null
                    && ticket.getCurrentAssignee().getId().equals(currentUser.getId());
            if (!isAssignedToMe) {
                auditLogger.ticketDenied(currentUser, ticket.getTicketNumber(), "ACCESS_ATTACHMENT", 404);
                throw new AppException(ErrorCode.TICKET_NOT_FOUND);
            }
        }
    }

    private AttachmentResponse toResponse(TicketAttachment a) {
        return new AttachmentResponse(
                a.getId(),
                a.getTicket().getId(),
                a.getFileName(),
                a.getFileSize(),
                a.getMimeType(),
                a.getUploader().getId(),
                a.getUploader().getFullName(),
                a.getCreatedAt()
        );
    }

    /**
     * Kiểm tra vài byte đầu của tệp có đúng chữ ký của loại tệp không (SecRS IA-HD03: "chữ ký nội dung").
     * docx/xlsx là tệp ZIP (bắt đầu bằng byte 50 4B 03 04, tức "PK.."); txt/log không có chữ ký nên chỉ chặn tệp nhị phân (có byte 0).
     */
    private boolean hasValidSignature(MultipartFile file, String ext) {
        byte[] head = new byte[8192];
        int n;
        try (InputStream is = file.getInputStream()) {
            n = is.readNBytes(head, 0, head.length);
        } catch (IOException e) {
            return false;
        }
        return switch (ext) {
            case "jpg", "jpeg" -> startsWith(head, n, 0xFF, 0xD8, 0xFF);
            case "png" -> startsWith(head, n, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "gif" -> startsWith(head, n, 'G', 'I', 'F', '8');
            case "pdf" -> startsWith(head, n, '%', 'P', 'D', 'F', '-');
            case "docx", "xlsx" -> startsWith(head, n, 'P', 'K', 0x03, 0x04);
            case "txt", "log" -> hasNoNullByte(head, n);
            default -> false;
        };
    }

    private static boolean startsWith(byte[] data, int length, int... signature) {
        if (length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((data[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasNoNullByte(byte[] data, int length) {
        for (int i = 0; i < length; i++) {
            if (data[i] == 0) {
                return false;
            }
        }
        return true;
    }

    private String getFileExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot + 1) : "";
    }

    private String sanitizeFileName(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private Ticket findTicket(String idOrNumber) {
        try {
            Long id = Long.parseLong(idOrNumber.trim());
            return ticketRepository.findById(id)
                    .orElseGet(() -> ticketRepository.findByTicketNumber(idOrNumber.trim())
                            .orElseThrow(() -> new AppException(ErrorCode.TICKET_NOT_FOUND)));
        } catch (NumberFormatException e) {
            return ticketRepository.findByTicketNumber(idOrNumber.trim())
                    .orElseThrow(() -> new AppException(ErrorCode.TICKET_NOT_FOUND));
        }
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return userRepository.findByUsername(auth.getName())
                    .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
        }
        throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
}
