package com.charging.repository;

import com.charging.entity.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class DataStore {
    private final Map<Long, Site> sites = new ConcurrentHashMap<>();
    private final Map<Long, ChargingPile> chargingPiles = new ConcurrentHashMap<>();
    private final Map<Long, User> users = new ConcurrentHashMap<>();
    private final Map<Long, Vehicle> vehicles = new ConcurrentHashMap<>();
    private final Map<Long, ElectricityPriceRule> priceRules = new ConcurrentHashMap<>();
    private final Map<Long, Reservation> reservations = new ConcurrentHashMap<>();
    private final Map<Long, ChargingRecord> chargingRecords = new ConcurrentHashMap<>();
    private final Map<Long, AbnormalOrder> abnormalOrders = new ConcurrentHashMap<>();
    private final Map<Long, DailyReport> dailyReports = new ConcurrentHashMap<>();

    private final AtomicLong siteIdCounter = new AtomicLong(1);
    private final AtomicLong pileIdCounter = new AtomicLong(1);
    private final AtomicLong userIdCounter = new AtomicLong(1);
    private final AtomicLong vehicleIdCounter = new AtomicLong(1);
    private final AtomicLong priceRuleIdCounter = new AtomicLong(1);
    private final AtomicLong reservationIdCounter = new AtomicLong(1);
    private final AtomicLong chargingRecordIdCounter = new AtomicLong(1);
    private final AtomicLong abnormalOrderIdCounter = new AtomicLong(1);
    private final AtomicLong dailyReportIdCounter = new AtomicLong(1);

    public DataStore() {
        initializeData();
    }

    private void initializeData() {
        Site site1 = Site.builder()
                .id(siteIdCounter.getAndIncrement())
                .name("朝阳公园充电站")
                .area("朝阳区")
                .address("北京市朝阳区朝阳公园西门")
                .openingTime(java.time.LocalTime.of(8, 0))
                .closingTime(java.time.LocalTime.of(22, 0))
                .serviceFeePerKwh(0.8)
                .active(true)
                .build();
        sites.put(site1.getId(), site1);

        Site site2 = Site.builder()
                .id(siteIdCounter.getAndIncrement())
                .name("海淀科技园充电站")
                .area("海淀区")
                .address("北京市海淀区中关村科技园")
                .openingTime(java.time.LocalTime.of(0, 0))
                .closingTime(java.time.LocalTime.of(23, 59))
                .serviceFeePerKwh(0.6)
                .active(true)
                .build();
        sites.put(site2.getId(), site2);

        for (int i = 1; i <= 4; i++) {
            ChargingPile pile = ChargingPile.builder()
                    .id(pileIdCounter.getAndIncrement())
                    .siteId(site1.getId())
                    .pileNo("CP" + String.format("%03d", i))
                    .name(site1.getName() + "充电桩" + i + "号")
                    .status(ChargingPile.PileStatus.IDLE)
                    .maxPower(120.0)
                    .active(true)
                    .build();
            chargingPiles.put(pile.getId(), pile);
        }

        for (int i = 5; i <= 8; i++) {
            ChargingPile pile = ChargingPile.builder()
                    .id(pileIdCounter.getAndIncrement())
                    .siteId(site2.getId())
                    .pileNo("CP" + String.format("%03d", i))
                    .name(site2.getName() + "充电桩" + (i - 4) + "号")
                    .status(ChargingPile.PileStatus.IDLE)
                    .maxPower(180.0)
                    .active(true)
                    .build();
            chargingPiles.put(pile.getId(), pile);
        }

        java.util.Set<java.time.DayOfWeek> workDays = java.util.EnumSet.of(
                java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.TUESDAY,
                java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.THURSDAY,
                java.time.DayOfWeek.FRIDAY
        );

        ElectricityPriceRule peak1 = ElectricityPriceRule.builder()
                .id(priceRuleIdCounter.getAndIncrement())
                .siteId(site1.getId())
                .ruleName("峰时电价(工作日)")
                .period(ElectricityPriceRule.PricePeriod.PEAK)
                .applicableDays(workDays)
                .startTime(java.time.LocalTime.of(10, 0))
                .endTime(java.time.LocalTime.of(14, 0))
                .pricePerKwh(java.math.BigDecimal.valueOf(1.2))
                .priority(1)
                .active(true)
                .build();
        priceRules.put(peak1.getId(), peak1);

        ElectricityPriceRule peak2 = ElectricityPriceRule.builder()
                .id(priceRuleIdCounter.getAndIncrement())
                .siteId(site1.getId())
                .ruleName("峰时电价2(工作日)")
                .period(ElectricityPriceRule.PricePeriod.PEAK)
                .applicableDays(workDays)
                .startTime(java.time.LocalTime.of(18, 0))
                .endTime(java.time.LocalTime.of(21, 0))
                .pricePerKwh(java.math.BigDecimal.valueOf(1.5))
                .priority(1)
                .active(true)
                .build();
        priceRules.put(peak2.getId(), peak2);

        ElectricityPriceRule flat = ElectricityPriceRule.builder()
                .id(priceRuleIdCounter.getAndIncrement())
                .siteId(site1.getId())
                .ruleName("平时电价(工作日)")
                .period(ElectricityPriceRule.PricePeriod.FLAT)
                .applicableDays(workDays)
                .startTime(java.time.LocalTime.of(7, 0))
                .endTime(java.time.LocalTime.of(10, 0))
                .pricePerKwh(java.math.BigDecimal.valueOf(0.8))
                .priority(2)
                .active(true)
                .build();
        priceRules.put(flat.getId(), flat);

        ElectricityPriceRule flat2 = ElectricityPriceRule.builder()
                .id(priceRuleIdCounter.getAndIncrement())
                .siteId(site1.getId())
                .ruleName("平时电价2(工作日)")
                .period(ElectricityPriceRule.PricePeriod.FLAT)
                .applicableDays(workDays)
                .startTime(java.time.LocalTime.of(14, 0))
                .endTime(java.time.LocalTime.of(18, 0))
                .pricePerKwh(java.math.BigDecimal.valueOf(0.8))
                .priority(2)
                .active(true)
                .build();
        priceRules.put(flat2.getId(), flat2);

        ElectricityPriceRule valley = ElectricityPriceRule.builder()
                .id(priceRuleIdCounter.getAndIncrement())
                .siteId(site1.getId())
                .ruleName("谷时电价(工作日)")
                .period(ElectricityPriceRule.PricePeriod.VALLEY)
                .applicableDays(workDays)
                .startTime(java.time.LocalTime.of(21, 0))
                .endTime(java.time.LocalTime.of(23, 59))
                .pricePerKwh(java.math.BigDecimal.valueOf(0.4))
                .priority(3)
                .active(true)
                .build();
        priceRules.put(valley.getId(), valley);

        User user1 = User.builder()
                .id(userIdCounter.getAndIncrement())
                .username("张三")
                .phone("13800138001")
                .balance(java.math.BigDecimal.valueOf(500.00))
                .frozenAmount(java.math.BigDecimal.ZERO)
                .creditStatus(User.CreditStatus.GOOD)
                .overdueCount(0)
                .build();
        users.put(user1.getId(), user1);

        User user2 = User.builder()
                .id(userIdCounter.getAndIncrement())
                .username("李四")
                .phone("13800138002")
                .balance(java.math.BigDecimal.valueOf(200.00))
                .frozenAmount(java.math.BigDecimal.ZERO)
                .creditStatus(User.CreditStatus.EXCELLENT)
                .overdueCount(0)
                .build();
        users.put(user2.getId(), user2);

        Vehicle vehicle1 = Vehicle.builder()
                .id(vehicleIdCounter.getAndIncrement())
                .userId(user1.getId())
                .plateNumber("京A12345")
                .brand("特斯拉")
                .model("Model 3")
                .batteryCapacity(75.0)
                .active(true)
                .build();
        vehicles.put(vehicle1.getId(), vehicle1);

        Vehicle vehicle2 = Vehicle.builder()
                .id(vehicleIdCounter.getAndIncrement())
                .userId(user2.getId())
                .plateNumber("京B67890")
                .brand("比亚迪")
                .model("汉EV")
                .batteryCapacity(85.0)
                .active(true)
                .build();
        vehicles.put(vehicle2.getId(), vehicle2);
    }

    public Long nextSiteId() { return siteIdCounter.getAndIncrement(); }
    public Long nextPileId() { return pileIdCounter.getAndIncrement(); }
    public Long nextUserId() { return userIdCounter.getAndIncrement(); }
    public Long nextVehicleId() { return vehicleIdCounter.getAndIncrement(); }
    public Long nextPriceRuleId() { return priceRuleIdCounter.getAndIncrement(); }
    public Long nextReservationId() { return reservationIdCounter.getAndIncrement(); }
    public Long nextChargingRecordId() { return chargingRecordIdCounter.getAndIncrement(); }
    public Long nextAbnormalOrderId() { return abnormalOrderIdCounter.getAndIncrement(); }
    public Long nextDailyReportId() { return dailyReportIdCounter.getAndIncrement(); }

    public Map<Long, Site> getSites() { return sites; }
    public Map<Long, ChargingPile> getChargingPiles() { return chargingPiles; }
    public Map<Long, User> getUsers() { return users; }
    public Map<Long, Vehicle> getVehicles() { return vehicles; }
    public Map<Long, ElectricityPriceRule> getPriceRules() { return priceRules; }
    public Map<Long, Reservation> getReservations() { return reservations; }
    public Map<Long, ChargingRecord> getChargingRecords() { return chargingRecords; }
    public Map<Long, AbnormalOrder> getAbnormalOrders() { return abnormalOrders; }
    public Map<Long, DailyReport> getDailyReports() { return dailyReports; }
}
