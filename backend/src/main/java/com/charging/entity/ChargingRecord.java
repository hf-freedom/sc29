package com.charging.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargingRecord {
    private Long id;
    private String recordNo;
    private Long reservationId;
    private Long userId;
    private Long siteId;
    private Long pileId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double totalKwh;
    private BigDecimal electricityFee;
    private BigDecimal serviceFee;
    private BigDecimal totalFee;
    private ChargingStatus status;
    private List<ChargingSegment> segments;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void addSegment(ChargingSegment segment) {
        if (this.segments == null) {
            this.segments = new ArrayList<>();
        }
        this.segments.add(segment);
    }

    public enum ChargingStatus {
        CHARGING,
        COMPLETED,
        ABNORMAL_STOPPED
    }
}
