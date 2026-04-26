package com.charging.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyReport {
    private Long id;
    private Long siteId;
    private LocalDate reportDate;
    private Integer totalReservations;
    private Integer completedReservations;
    private Integer cancelledReservations;
    private Integer totalChargingRecords;
    private Double totalKwh;
    private BigDecimal totalElectricityFee;
    private BigDecimal totalServiceFee;
    private BigDecimal totalRevenue;
    private Integer abnormalCount;
    private Integer abnormalResolved;
    private BigDecimal totalCompensation;
    private LocalDateTime createdAt;
}
