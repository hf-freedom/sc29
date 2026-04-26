package com.charging.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Site {
    private Long id;
    private String name;
    private String area;
    private String address;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private Double serviceFeePerKwh;
    private Boolean active;
}
