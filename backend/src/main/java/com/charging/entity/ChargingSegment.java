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
public class ChargingSegment {
    private Long id;
    private Long chargingRecordId;
    private ElectricityPriceRule.PricePeriod period;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double kwh;
    private BigDecimal pricePerKwh;
    private BigDecimal segmentFee;
}
