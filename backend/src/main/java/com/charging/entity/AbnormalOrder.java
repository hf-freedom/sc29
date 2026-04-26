package com.charging.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbnormalOrder {
    private Long id;
    private String abnormalNo;
    private Long reservationId;
    private Long chargingRecordId;
    private Long userId;
    private Long siteId;
    private Long pileId;
    private AbnormalType type;
    private AbnormalStatus status;
    private LocalDateTime occurTime;
    private LocalDateTime resolveTime;
    private Double chargedKwh;
    private BigDecimal compensatedAmount;
    private String description;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum AbnormalType {
        PILE_FAULT,
        POWER_OUTAGE,
        NETWORK_ERROR,
        OTHER
    }

    public enum AbnormalStatus {
        PENDING,
        PROCESSING,
        RESOLVED,
        COMPENSATED
    }
}
