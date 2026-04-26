package com.charging.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargingPile {
    private Long id;
    private Long siteId;
    private String pileNo;
    private String name;
    private PileStatus status;
    private Double maxPower;
    private Boolean active;

    public enum PileStatus {
        IDLE,
        RESERVED,
        CHARGING,
        FAULT,
        OFFLINE
    }
}
