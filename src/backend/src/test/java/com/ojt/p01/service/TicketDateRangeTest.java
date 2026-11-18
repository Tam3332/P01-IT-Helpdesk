package com.ojt.p01.service;

import com.ojt.p01.exception.AppException;
import com.ojt.p01.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test lọc theo khoảng ngày tạo (HD13, SDD §4; task T5-06): đổi ngày người dùng chọn thành mốc giờ Việt Nam.
 */
@DisplayName("TicketDateRangeTest - Lọc ticket theo khoảng ngày tạo")
class TicketDateRangeTest {

    @Test
    @DisplayName("Từ ngày và đến ngày: từ 00:00 ngày đầu (UTC+7) đến trước 00:00 ngày sau ngày cuối")
    void bothDates_fullDays() {
        OffsetDateTime[] r = TicketService.createdRange(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 16));
        assertEquals(OffsetDateTime.parse("2026-11-10T00:00+07:00"), r[0]);
        assertEquals(OffsetDateTime.parse("2026-11-17T00:00+07:00"), r[1]);
    }

    @Test
    @DisplayName("Cùng một ngày: lấy trọn 24 giờ của ngày đó")
    void sameDay_oneFullDay() {
        OffsetDateTime[] r = TicketService.createdRange(LocalDate.of(2026, 11, 17), LocalDate.of(2026, 11, 17));
        assertEquals(OffsetDateTime.parse("2026-11-17T00:00+07:00"), r[0]);
        assertEquals(OffsetDateTime.parse("2026-11-18T00:00+07:00"), r[1]);
    }

    @Test
    @DisplayName("Chỉ có một đầu: đầu còn lại không giới hạn")
    void oneSideOnly() {
        assertNull(TicketService.createdRange(LocalDate.of(2026, 11, 10), null)[1]);
        assertNull(TicketService.createdRange(null, LocalDate.of(2026, 11, 10))[0]);
        OffsetDateTime[] none = TicketService.createdRange(null, null);
        assertNull(none[0]);
        assertNull(none[1]);
    }

    @Test
    @DisplayName("Từ ngày sau đến ngày → 400 VALIDATION_FAILED")
    void fromAfterTo_rejected() {
        AppException e = assertThrows(AppException.class,
                () -> TicketService.createdRange(LocalDate.of(2026, 11, 17), LocalDate.of(2026, 11, 10)));
        assertEquals(ErrorCode.VALIDATION_FAILED, e.getErrorCode());
    }
}
