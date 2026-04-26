package com.charging.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String username;
    private String phone;
    private BigDecimal balance;
    private BigDecimal frozenAmount;
    private CreditStatus creditStatus;
    private Integer overdueCount;

    public enum CreditStatus {
        EXCELLENT,
        GOOD,
        NORMAL,
        POOR,
        BLACKLIST
    }
}
