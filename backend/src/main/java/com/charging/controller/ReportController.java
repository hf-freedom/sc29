package com.charging.controller;

import com.charging.dto.ApiResponse;
import com.charging.entity.AbnormalOrder;
import com.charging.entity.DailyReport;
import com.charging.repository.DataStore;
import com.charging.service.ChargingService;
import com.charging.service.ScheduledTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    @Autowired
    private ScheduledTaskService scheduledTaskService;

    @Autowired
    private ChargingService chargingService;

    @Autowired
    private DataStore dataStore;

    @GetMapping("/daily")
    public ApiResponse<List<DailyReport>> getAllDailyReports() {
        List<DailyReport> reports = scheduledTaskService.getAllDailyReports();
        return ApiResponse.success(reports);
    }

    @GetMapping("/daily/site/{siteId}")
    public ApiResponse<List<DailyReport>> getDailyReportsBySite(@PathVariable Long siteId) {
        List<DailyReport> reports = scheduledTaskService.getDailyReportsBySite(siteId);
        return ApiResponse.success(reports);
    }

    @GetMapping("/abnormal")
    public ApiResponse<List<AbnormalOrder>> getAllAbnormalOrders() {
        List<AbnormalOrder> orders = dataStore.getAbnormalOrders().values().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
        return ApiResponse.success(orders);
    }

    @GetMapping("/abnormal/pending")
    public ApiResponse<List<AbnormalOrder>> getPendingAbnormalOrders() {
        List<AbnormalOrder> orders = dataStore.getAbnormalOrders().values().stream()
                .filter(o -> o.getStatus() == AbnormalOrder.AbnormalStatus.PENDING ||
                        o.getStatus() == AbnormalOrder.AbnormalStatus.PROCESSING)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
        return ApiResponse.success(orders);
    }

    @PostMapping("/abnormal/{abnormalOrderId}/resolve")
    public ApiResponse<AbnormalOrder> resolveAbnormalOrder(
            @PathVariable Long abnormalOrderId,
            @RequestParam(required = false, defaultValue = "false") boolean compensate,
            @RequestParam(required = false) String remark) {
        try {
            AbnormalOrder order = chargingService.resolveAbnormalOrder(abnormalOrderId, compensate, remark);
            return ApiResponse.success("异常单已处理", order);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
