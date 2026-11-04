package com.ojt.p01.service;

import com.ojt.p01.entity.TicketStatus;
import com.ojt.p01.exception.AppException;
import com.ojt.p01.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Trình kiểm tra chuyển trạng thái vé hợp lệ (State Machine Validator).
 *
 * Áp dụng bảng chuyển trạng thái trong TicketStatus Javadoc.
 * Dùng trong các service: AssignTicketService (T6), UpdateStatusService (T5), ReopenService (T7).
 *
 * Cách dùng:
 *   transitionValidator.assertTransitionAllowed(currentStatus, newStatus);
 *   // ném AppException INVALID_STATUS_TRANSITION nếu không hợp lệ.
 */
@Component
public class TicketStatusTransitionValidator {

    /**
     * Bảng chuyển trạng thái hợp lệ.
     * Key   = trạng thái hiện tại
     * Value = tập trạng thái có thể chuyển đến
     */
    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED_TRANSITIONS;

    static {
        ALLOWED_TRANSITIONS = new EnumMap<>(TicketStatus.class);

        // NEW → ASSIGNED (IT_MANAGER phân công — HD06)
        ALLOWED_TRANSITIONS.put(TicketStatus.NEW,
                EnumSet.of(TicketStatus.ASSIGNED));

        // ASSIGNED → IN_PROGRESS (IT_STAFF nhận xử lý — HD07)
        ALLOWED_TRANSITIONS.put(TicketStatus.ASSIGNED,
                EnumSet.of(TicketStatus.IN_PROGRESS));

        // IN_PROGRESS → RESOLVED (IT_STAFF hoàn thành, bắt buộc resolution_note — HD07)
        ALLOWED_TRANSITIONS.put(TicketStatus.IN_PROGRESS,
                EnumSet.of(TicketStatus.RESOLVED));

        // RESOLVED → CLOSED hoặc REOPEN_PENDING
        ALLOWED_TRANSITIONS.put(TicketStatus.RESOLVED,
                EnumSet.of(TicketStatus.CLOSED, TicketStatus.REOPEN_PENDING));

        // REOPEN_PENDING → REOPENED (duyệt) hoặc RESOLVED (từ chối)
        ALLOWED_TRANSITIONS.put(TicketStatus.REOPEN_PENDING,
                EnumSet.of(TicketStatus.REOPENED, TicketStatus.RESOLVED));

        // REOPENED → IN_PROGRESS (IT_STAFF tiếp tục)
        ALLOWED_TRANSITIONS.put(TicketStatus.REOPENED,
                EnumSet.of(TicketStatus.IN_PROGRESS));

        // CLOSED → không thể chuyển (terminal state)
        ALLOWED_TRANSITIONS.put(TicketStatus.CLOSED,
                EnumSet.noneOf(TicketStatus.class));
    }

    /**
     * Kiểm tra transition hợp lệ. Ném AppException INVALID_STATUS_TRANSITION nếu không hợp lệ.
     *
     * @param current Trạng thái hiện tại của vé
     * @param target  Trạng thái muốn chuyển đến
     */
    public void assertTransitionAllowed(TicketStatus current, TicketStatus target) {
        Set<TicketStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, EnumSet.noneOf(TicketStatus.class));
        if (!allowed.contains(target)) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION,
                    String.format("Không thể chuyển từ trạng thái %s sang %s", current.name(), target.name()));
        }
    }

    /**
     * Kiểm tra không ném exception — tiện dùng trong logic phân nhánh.
     */
    public boolean isTransitionAllowed(TicketStatus current, TicketStatus target) {
        return ALLOWED_TRANSITIONS
                .getOrDefault(current, EnumSet.noneOf(TicketStatus.class))
                .contains(target);
    }

    /**
     * Trả về tập trạng thái có thể chuyển đến từ trạng thái hiện tại.
     */
    public Set<TicketStatus> getAllowedTransitions(TicketStatus current) {
        return ALLOWED_TRANSITIONS.getOrDefault(current, EnumSet.noneOf(TicketStatus.class));
    }
}
