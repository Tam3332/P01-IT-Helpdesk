package com.ojt.p01.service;

import com.ojt.p01.dto.response.NotificationResponse;
import com.ojt.p01.entity.Ticket;
import com.ojt.p01.entity.TicketHistory;
import com.ojt.p01.entity.User;
import com.ojt.p01.exception.AppException;
import com.ojt.p01.exception.ErrorCode;
import com.ojt.p01.repository.TicketHistoryRepository;
import com.ojt.p01.repository.TicketRepository;
import com.ojt.p01.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationService {

    private final TicketRepository ticketRepository;
    private final TicketHistoryRepository historyRepository;
    private final UserRepository userRepository;

    public NotificationService(TicketRepository ticketRepository,
                               TicketHistoryRepository historyRepository,
                               UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
    }

    /**
     * Lấy danh sách thông báo chính xác theo từng vai trò:
     * 1. IT_MANAGER / ADMIN: Hiện thông báo khi có ticket MỚI (NEW) do Employee tạo hoặc mở lại (REOPENED, REOPEN_PENDING).
     * 2. IT_STAFF: Chỉ hiện ticket khi Manager phân công cho mình (ASSIGNED).
     * 3. EMPLOYEE: Chỉ hiện ticket khi được chuyển sang trạng thái Solved (RESOLVED) do Staff bấm Submit/Resolve.
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(int limit) {
        User currentUser = getCurrentUser();
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "EMPLOYEE";
        int safeLimit = Math.min(Math.max(limit, 1), 50);

        List<NotificationResponse> result = new ArrayList<>();

        if ("IT_MANAGER".equalsIgnoreCase(roleName) || "ADMIN".equalsIgnoreCase(roleName)) {
            // Manager: Chỉ hiện các vé MỚI (NEW) hoặc yêu cầu MỞ LẠI (REOPEN_PENDING, REOPENED)
            List<Ticket> managerTickets = ticketRepository.findNotificationsForManager(PageRequest.of(0, safeLimit));
            for (Ticket t : managerTickets) {
                result.add(new NotificationResponse(
                        t.getId(),
                        t.getId(),
                        t.getTicketNumber(),
                        t.getTitle(),
                        t.getStatus(), // "NEW", "REOPENED", "REOPEN_PENDING"
                        "NEW".equals(t.getStatus()) ? "TICKET_CREATED" : "TICKET_REOPENED",
                        t.getRequester() != null ? t.getRequester().getFullName() : "Nhân viên",
                        t.getUpdatedAt() != null ? t.getUpdatedAt() : t.getCreatedAt()
                ));
            }

        } else if ("IT_STAFF".equalsIgnoreCase(roleName)) {
            // Staff: Chỉ hiện ticket khi Manager phân công cho mình
            List<TicketHistory> staffHistories = historyRepository.findNotificationsForStaff(
                    String.valueOf(currentUser.getId()), PageRequest.of(0, safeLimit));

            for (TicketHistory h : staffHistories) {
                Ticket t = h.getTicket();
                result.add(new NotificationResponse(
                        h.getId(),
                        t.getId(),
                        t.getTicketNumber(),
                        t.getTitle(),
                        "ASSIGNED", // Hiển thị rõ trạng thái: Đã phân công
                        h.getAction(),
                        h.getActor() != null ? h.getActor().getFullName() : "IT Manager",
                        h.getTimestamp()
                ));
            }

        } else {
            // Employee: Chỉ hiện ticket khi chuyển sang trạng thái Solved (RESOLVED) khi Staff bấm Submit/Resolve
            List<TicketHistory> empHistories = historyRepository.findNotificationsForEmployee(
                    currentUser.getId(), PageRequest.of(0, safeLimit));

            for (TicketHistory h : empHistories) {
                Ticket t = h.getTicket();
                result.add(new NotificationResponse(
                        h.getId(),
                        t.getId(),
                        t.getTicketNumber(),
                        t.getTitle(),
                        "RESOLVED", // Hiển thị rõ trạng thái: Đã giải quyết
                        "RESOLVED",
                        h.getActor() != null ? h.getActor().getFullName() : "IT Staff",
                        h.getTimestamp()
                ));
            }
        }

        return result;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            String principal = auth.getName();
            return userRepository.findByUsername(principal)
                    .orElseGet(() -> userRepository.findByEmail(principal)
                            .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED)));
        }
        throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
}
