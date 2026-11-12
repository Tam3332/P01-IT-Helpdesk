package com.ojt.p01.service;

import com.ojt.p01.entity.TicketStatus;
import com.ojt.p01.exception.AppException;
import com.ojt.p01.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TicketStatusTransitionValidator - Kiểm thử chuyển trạng thái vé")
class TicketStatusTransitionValidatorTest {

    private TicketStatusTransitionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TicketStatusTransitionValidator();
    }

    @Nested
    @DisplayName("Các cặp chuyển trạng thái HỢP LỆ")
    class ValidTransitionsTest {

        @ParameterizedTest(name = "{0} -> {1} phải hợp lệ")
        @CsvSource({
                "NEW, ASSIGNED",
                "ASSIGNED, IN_PROGRESS",
                "IN_PROGRESS, RESOLVED",
                "RESOLVED, CLOSED",
                "RESOLVED, REOPEN_PENDING",
                "REOPEN_PENDING, REOPENED",
                "REOPEN_PENDING, RESOLVED",
                "REOPENED, IN_PROGRESS",
                "REOPENED, ASSIGNED"
        })
        void shouldAllowValidTransitions(TicketStatus from, TicketStatus to) {
            assertThat(validator.isTransitionAllowed(from, to)).isTrue();
            // assertTransitionAllowed không được ném ngoại lệ
            validator.assertTransitionAllowed(from, to);
        }
    }

    @Nested
    @DisplayName("Các cặp chuyển trạng thái KHÔNG HỢP LỆ")
    class InvalidTransitionsTest {

        @ParameterizedTest(name = "{0} -> {1} phải bị chặn")
        @CsvSource({
                "NEW, IN_PROGRESS",
                "NEW, RESOLVED",
                "NEW, CLOSED",
                "NEW, REOPEN_PENDING",
                "NEW, REOPENED",
                "ASSIGNED, NEW",
                "ASSIGNED, RESOLVED",
                "ASSIGNED, CLOSED",
                "ASSIGNED, REOPEN_PENDING",
                "IN_PROGRESS, NEW",
                "IN_PROGRESS, ASSIGNED",
                "IN_PROGRESS, CLOSED",
                "IN_PROGRESS, REOPENED",
                "RESOLVED, NEW",
                "RESOLVED, ASSIGNED",
                "RESOLVED, IN_PROGRESS",
                "RESOLVED, REOPENED",
                "REOPEN_PENDING, NEW",
                "REOPEN_PENDING, ASSIGNED",
                "REOPEN_PENDING, IN_PROGRESS",
                "REOPEN_PENDING, CLOSED",
                "REOPENED, NEW",
                "REOPENED, RESOLVED",
                "REOPENED, CLOSED",
                "REOPENED, REOPEN_PENDING"
        })
        void shouldRejectInvalidTransitions(TicketStatus from, TicketStatus to) {
            assertThat(validator.isTransitionAllowed(from, to)).isFalse();
            assertThatThrownBy(() -> validator.assertTransitionAllowed(from, to))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> {
                        AppException appEx = (AppException) e;
                        assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
                    });
        }

        @ParameterizedTest(name = "CLOSED không thể chuyển sang {0}")
        @EnumSource(TicketStatus.class)
        void closedIsTerminalState(TicketStatus anyTarget) {
            assertThat(validator.isTransitionAllowed(TicketStatus.CLOSED, anyTarget)).isFalse();
            assertThatThrownBy(() -> validator.assertTransitionAllowed(TicketStatus.CLOSED, anyTarget))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> {
                        AppException appEx = (AppException) e;
                        assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
                    });
        }
    }

    @Nested
    @DisplayName("Kiểm tra tập hợp getAllowedTransitions")
    class AllowedTransitionsSetTest {

        @Test
        void shouldReturnExpectedSetForNew() {
            Set<TicketStatus> allowed = validator.getAllowedTransitions(TicketStatus.NEW);
            assertThat(allowed).containsExactly(TicketStatus.ASSIGNED);
        }

        @Test
        void shouldReturnExpectedSetForAssigned() {
            Set<TicketStatus> allowed = validator.getAllowedTransitions(TicketStatus.ASSIGNED);
            assertThat(allowed).containsExactly(TicketStatus.IN_PROGRESS);
        }

        @Test
        void shouldReturnExpectedSetForInProgress() {
            Set<TicketStatus> allowed = validator.getAllowedTransitions(TicketStatus.IN_PROGRESS);
            assertThat(allowed).containsExactly(TicketStatus.RESOLVED);
        }

        @Test
        void shouldReturnExpectedSetForResolved() {
            Set<TicketStatus> allowed = validator.getAllowedTransitions(TicketStatus.RESOLVED);
            assertThat(allowed).containsExactlyInAnyOrder(TicketStatus.CLOSED, TicketStatus.REOPEN_PENDING);
        }

        @Test
        void shouldReturnExpectedSetForReopenPending() {
            Set<TicketStatus> allowed = validator.getAllowedTransitions(TicketStatus.REOPEN_PENDING);
            assertThat(allowed).containsExactlyInAnyOrder(TicketStatus.REOPENED, TicketStatus.RESOLVED);
        }

        @Test
        void shouldReturnExpectedSetForReopened() {
            Set<TicketStatus> allowed = validator.getAllowedTransitions(TicketStatus.REOPENED);
            assertThat(allowed).containsExactlyInAnyOrder(TicketStatus.IN_PROGRESS, TicketStatus.ASSIGNED);
        }

        @Test
        void shouldReturnEmptySetForClosed() {
            Set<TicketStatus> allowed = validator.getAllowedTransitions(TicketStatus.CLOSED);
            assertThat(allowed).isEmpty();
        }
    }
}
