package com.ojt.p01.audit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojt.p01.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AuditLoggerTest {

    @TempDir
    Path tempDir;

    private Path logFile;
    private AuditLogger auditLogger;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        logFile = tempDir.resolve("test_audit.jsonl");
        objectMapper = new ObjectMapper();
        UserRepository userRepository = mock(UserRepository.class);
        auditLogger = new AuditLogger(objectMapper, userRepository, logFile.toString());
    }

    @AfterEach
    void tearDown() {
        auditLogger.shutdown();
    }

    @Test
    @DisplayName("Ghi log sự kiện có before và after (TICKET_ASSIGNED)")
    void shouldLogTicketAssignedWithBeforeAndAfter() throws Exception {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("assignee_id", null);

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("assignee_id", 3L);

        auditLogger.log("TICKET_ASSIGNED", 4L, "IT_MANAGER", "TICKET", "HD-2026-0001",
                "ASSIGN", "SUCCESS", 200, before, after);

        // Đợi writer ghi xuống file
        Thread.sleep(150);

        List<String> lines = Files.readAllLines(logFile);
        assertThat(lines).hasSize(1);

        Map<String, Object> parsed = objectMapper.readValue(lines.get(0), new TypeReference<>() {});
        assertThat(parsed.get("event_type")).isEqualTo("TICKET_ASSIGNED");
        assertThat(parsed.get("actor_id")).isEqualTo(4);
        assertThat(parsed.get("role")).isEqualTo("IT_MANAGER");
        assertThat(parsed.get("target_type")).isEqualTo("TICKET");
        assertThat(parsed.get("target_id")).isEqualTo("HD-2026-0001");
        assertThat(parsed.get("action")).isEqualTo("ASSIGN");
        assertThat(parsed.get("outcome")).isEqualTo("SUCCESS");
        assertThat(parsed.get("http_status")).isEqualTo(200);

        @SuppressWarnings("unchecked")
        Map<String, Object> parsedBefore = (Map<String, Object>) parsed.get("before");
        assertThat(parsedBefore).isNotNull();
        assertThat(parsedBefore.get("assignee_id")).isNull();

        @SuppressWarnings("unchecked")
        Map<String, Object> parsedAfter = (Map<String, Object>) parsed.get("after");
        assertThat(parsedAfter).isNotNull();
        assertThat(parsedAfter.get("assignee_id")).isEqualTo(3);
    }

    @Test
    @DisplayName("Ghi log sự kiện đổi trạng thái (TICKET_STATUS_CHANGED)")
    void shouldLogStatusChangedWithBeforeAndAfter() throws Exception {
        Map<String, Object> before = Map.of("status", "ASSIGNED");
        Map<String, Object> after = Map.of("status", "IN_PROGRESS");

        auditLogger.log("TICKET_STATUS_CHANGED", 3L, "IT_STAFF", "TICKET", "HD-2026-0001",
                "STATUS_CHANGE", "SUCCESS", 200, before, after);

        Thread.sleep(150);

        List<String> lines = Files.readAllLines(logFile);
        assertThat(lines).hasSize(1);

        Map<String, Object> parsed = objectMapper.readValue(lines.get(0), new TypeReference<>() {});
        assertThat(parsed.get("event_type")).isEqualTo("TICKET_STATUS_CHANGED");
        assertThat(parsed.get("before")).isEqualTo(Map.of("status", "ASSIGNED"));
        assertThat(parsed.get("after")).isEqualTo(Map.of("status", "IN_PROGRESS"));
    }

    @Test
    @DisplayName("Sự kiện không có before/after thì không xuất hiện 2 trường này trong JSON")
    void shouldOmitBeforeAndAfterWhenNull() throws Exception {
        auditLogger.log("LOGIN_SUCCESS", 1L, "EMPLOYEE", "USER", "1", "LOGIN", "SUCCESS", 200);

        Thread.sleep(150);

        List<String> lines = Files.readAllLines(logFile);
        assertThat(lines).hasSize(1);

        Map<String, Object> parsed = objectMapper.readValue(lines.get(0), new TypeReference<>() {});
        assertThat(parsed.get("event_type")).isEqualTo("LOGIN_SUCCESS");
        assertThat(parsed).doesNotContainKey("before");
        assertThat(parsed).doesNotContainKey("after");
    }
}
