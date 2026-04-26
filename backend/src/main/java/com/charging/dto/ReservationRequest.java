package com.charging.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequest {
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    @NotNull(message = "车辆ID不能为空")
    private Long vehicleId;
    
    @NotNull(message = "充电桩ID不能为空")
    private Long pileId;
    
    @NotNull(message = "预约开始时间不能为空")
    private LocalDateTime startTime;
    
    @NotNull(message = "预约结束时间不能为空")
    private LocalDateTime endTime;
    
    private String remark;
}
