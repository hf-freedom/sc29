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
public class Reservation {
    private Long id;
    private String reservationNo;
    private Long userId;
    private Long vehicleId;
    private Long siteId;
    private Long pileId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime expectedCheckInTime;
    private LocalDateTime actualCheckInTime;
    private LocalDateTime checkOutTime;
    private ReservationStatus status;
    private BigDecimal depositAmount;
    private BigDecimal depositDeducted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String remark;

    public enum ReservationStatus {
        PENDING,
        RESERVED,
        CHECKED_IN,
        CHARGING,
        COMPLETED,
        CANCELLED,
        TIMEOUT_CANCELLED,
        ABNORMAL
    }
}
