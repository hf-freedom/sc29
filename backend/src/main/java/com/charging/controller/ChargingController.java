package com.charging.controller;

import com.charging.dto.ApiResponse;
import com.charging.dto.StartChargingRequest;
import com.charging.entity.AbnormalOrder;
import com.charging.entity.ChargingRecord;
import com.charging.service.ChargingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/charging")
@CrossOrigin(origins = "*")
public class ChargingController {

    @Autowired
    private ChargingService chargingService;

    @GetMapping("/user/{userId}")
    public ApiResponse<List<ChargingRecord>> getChargingRecordsByUser(@PathVariable Long userId) {
        List<ChargingRecord> records = chargingService.getChargingRecordsByUser(userId);
        return ApiResponse.success(records);
    }

    @GetMapping("/records/{id}")
    public ApiResponse<ChargingRecord> getChargingRecordById(@PathVariable Long id) {
        ChargingRecord record = chargingService.getChargingRecordById(id);
        if (record == null) {
            return ApiResponse.error("充电记录不存在");
        }
        return ApiResponse.success(record);
    }

    @GetMapping("/sessions/active")
    public ApiResponse<List<ChargingService.ChargingSession>> getActiveSessions() {
        List<ChargingService.ChargingSession> sessions = chargingService.getAllActiveSessions();
        return ApiResponse.success(sessions);
    }

    @GetMapping("/sessions/{reservationId}")
    public ApiResponse<ChargingService.ChargingSession> getActiveSession(@PathVariable Long reservationId) {
        ChargingService.ChargingSession session = chargingService.getActiveSession(reservationId);
        if (session == null) {
            return ApiResponse.error("没有进行中的充电会话");
        }
        return ApiResponse.success(session);
    }

    @PostMapping("/start")
    public ApiResponse<ChargingRecord> startCharging(@Valid @RequestBody StartChargingRequest request) {
        try {
            ChargingRecord record = chargingService.startCharging(request.getReservationId());
            return ApiResponse.success("开始充电成功", record);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/stop/{reservationId}")
    public ApiResponse<ChargingRecord> stopCharging(@PathVariable Long reservationId) {
        try {
            ChargingRecord record = chargingService.stopCharging(reservationId);
            return ApiResponse.success("结束充电成功", record);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/fault/{reservationId}")
    public ApiResponse<AbnormalOrder> handlePileFault(
            @PathVariable Long reservationId,
            @RequestParam(required = false) String description) {
        try {
            AbnormalOrder abnormalOrder = chargingService.handlePileFault(reservationId, description);
            return ApiResponse.success("已记录异常并停止充电", abnormalOrder);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
