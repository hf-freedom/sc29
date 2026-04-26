package com.charging.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {
    private Long id;
    private Long userId;
    private String plateNumber;
    private String brand;
    private String model;
    private Double batteryCapacity;
    private Boolean active;
}
