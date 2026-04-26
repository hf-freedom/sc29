package com.charging.service;

import com.charging.entity.*;
import com.charging.repository.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScheduledTaskService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskService.class);

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private DataStore dataStore;

    @Scheduled(fixedRate = 60000)
    public void checkTimeoutReservations() {
        logger.info("执行超时预约检查任务...");
        List<Reservation> activeReservations = reservationService.getActiveReservations();
        
        for (Reservation reservation : activeReservations) {
            try {
                reservationService.processTimeoutReservation(reservation);
                if (reservation.getStatus() == Reservation.ReservationStatus.TIMEOUT_CANCELLED) {
                    logger.info("预约 {} 已超时取消", reservation.getReservationNo());
                }
            } catch (Exception e) {
                logger.error("处理预约 {} 时发生错误: {}", reservation.getId(), e.getMessage());
            }
        }
        logger.info("超时预约检查任务完成");
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void generateDailyReports() {
        logger.info("开始生成日报表...");
        LocalDate reportDate = LocalDate.now().minusDays(1);
        
        for (Site site : dataStore.getSites().values()) {
            try {
                DailyReport report = generateSiteDailyReport(site.getId(), reportDate);
                dataStore.getDailyReports().put(report.getId(), report);
                logger.info("已生成站点 {} 日报表: {}", site.getName(), reportDate);
            } catch (Exception e) {
                logger.error("生成站点 {} 日报表时出错: {}", site.getId(), e.getMessage());
            }
        }
        
        logger.info("日报表生成完成");
    }

    private DailyReport generateSiteDailyReport(Long siteId, LocalDate reportDate) {
        LocalDateTime dayStart = reportDate.atStartOfDay();
        LocalDateTime dayEnd = reportDate.plusDays(1).atStartOfDay();

        List<Reservation> siteReservations = dataStore.getReservations().values().stream()
                .filter(r -> r.getSiteId().equals(siteId))
                .filter(r -> r.getCreatedAt().isAfter(dayStart) && r.getCreatedAt().isBefore(dayEnd))
                .collect(Collectors.toList());

        long totalReservations = siteReservations.size();
        long completedReservations = siteReservations.stream()
                .filter(r -> r.getStatus() == Reservation.ReservationStatus.COMPLETED)
                .count();
        long cancelledReservations = siteReservations.stream()
                .filter(r -> r.getStatus() == Reservation.ReservationStatus.CANCELLED ||
                        r.getStatus() == Reservation.ReservationStatus.TIMEOUT_CANCELLED)
                .count();

        List<ChargingRecord> siteChargingRecords = dataStore.getChargingRecords().values().stream()
                .filter(r -> r.getSiteId().equals(siteId))
                .filter(r -> r.getCreatedAt().isAfter(dayStart) && r.getCreatedAt().isBefore(dayEnd))
                .collect(Collectors.toList());

        long totalChargingRecords = siteChargingRecords.size();
        double totalKwh = siteChargingRecords.stream()
                .mapToDouble(r -> r.getTotalKwh() != null ? r.getTotalKwh() : 0.0)
                .sum();

        BigDecimal totalElectricityFee = siteChargingRecords.stream()
                .map(r -> r.getElectricityFee() != null ? r.getElectricityFee() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalServiceFee = siteChargingRecords.stream()
                .map(r -> r.getServiceFee() != null ? r.getServiceFee() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<AbnormalOrder> siteAbnormalOrders = dataStore.getAbnormalOrders().values().stream()
                .filter(o -> o.getSiteId().equals(siteId))
                .filter(o -> o.getCreatedAt().isAfter(dayStart) && o.getCreatedAt().isBefore(dayEnd))
                .collect(Collectors.toList());

        long abnormalCount = siteAbnormalOrders.size();
        long abnormalResolved = siteAbnormalOrders.stream()
                .filter(o -> o.getStatus() == AbnormalOrder.AbnormalStatus.RESOLVED ||
                        o.getStatus() == AbnormalOrder.AbnormalStatus.COMPENSATED)
                .count();

        BigDecimal totalCompensation = siteAbnormalOrders.stream()
                .map(o -> o.getCompensatedAmount() != null ? o.getCompensatedAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DailyReport.builder()
                .id(dataStore.nextDailyReportId())
                .siteId(siteId)
                .reportDate(reportDate)
                .totalReservations((int) totalReservations)
                .completedReservations((int) completedReservations)
                .cancelledReservations((int) cancelledReservations)
                .totalChargingRecords((int) totalChargingRecords)
                .totalKwh(totalKwh)
                .totalElectricityFee(totalElectricityFee)
                .totalServiceFee(totalServiceFee)
                .totalRevenue(totalElectricityFee.add(totalServiceFee))
                .abnormalCount((int) abnormalCount)
                .abnormalResolved((int) abnormalResolved)
                .totalCompensation(totalCompensation)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public List<DailyReport> getDailyReportsBySite(Long siteId) {
        return dataStore.getDailyReports().values().stream()
                .filter(r -> r.getSiteId().equals(siteId))
                .sorted((a, b) -> b.getReportDate().compareTo(a.getReportDate()))
                .collect(Collectors.toList());
    }

    public List<DailyReport> getAllDailyReports() {
        return dataStore.getDailyReports().values().stream()
                .sorted((a, b) -> b.getReportDate().compareTo(a.getReportDate()))
                .collect(Collectors.toList());
    }
}
