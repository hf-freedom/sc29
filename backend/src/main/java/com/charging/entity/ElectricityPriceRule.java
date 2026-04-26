package com.charging.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElectricityPriceRule {
    private Long id;
    private Long siteId;
    private String ruleName;
    private PricePeriod period;
    private Set<DayOfWeek> applicableDays;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal pricePerKwh;
    private Integer priority;
    private Boolean active;

    public enum PricePeriod {
        PEAK,
        FLAT,
        VALLEY
    }
}
