package com.ojt.p01.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Entity mapping bảng sla_configs.
 */
@Entity
@Table(name = "sla_configs")
public class SLAConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

 /** LOW | MEDIUM | HIGH | CRITICAL — unique per level */
    @Column(name = "priority_level", nullable = false, unique = true, length = 20)
    private String priorityLevel;

 /** Thời gian phản hồi lần đầu tối đa (giờ) */
    @Column(name = "response_time_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal responseTimeHours;

 /** Thời gian giải quyết ticket tối đa (giờ) */
    @Column(name = "resolution_time_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal resolutionTimeHours;

 /** Ngưỡng % cảnh báo SLA sắp vi phạm (mặc định 80%) */
    @Column(name = "warning_threshold_pct", nullable = false)
    private Integer warningThresholdPct;

 /** Ngưỡng % đánh dấu SLA bị vi phạm (mặc định 100%) */
    @Column(name = "breach_threshold_pct", nullable = false)
    private Integer breachThresholdPct;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public SLAConfig() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(String priorityLevel) { this.priorityLevel = priorityLevel; }

    public BigDecimal getResponseTimeHours() { return responseTimeHours; }
    public void setResponseTimeHours(BigDecimal responseTimeHours) { this.responseTimeHours = responseTimeHours; }

    public BigDecimal getResolutionTimeHours() { return resolutionTimeHours; }
    public void setResolutionTimeHours(BigDecimal resolutionTimeHours) { this.resolutionTimeHours = resolutionTimeHours; }

    public Integer getWarningThresholdPct() { return warningThresholdPct; }
    public void setWarningThresholdPct(Integer warningThresholdPct) { this.warningThresholdPct = warningThresholdPct; }

    public Integer getBreachThresholdPct() { return breachThresholdPct; }
    public void setBreachThresholdPct(Integer breachThresholdPct) { this.breachThresholdPct = breachThresholdPct; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
