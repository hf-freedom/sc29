package com.charging.service;

import com.charging.entity.*;
import com.charging.repository.DataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ChargingService {

    @Autowired
    private DataStore dataStore;

    private final Map<Long, ChargingSession> activeChargingSessions = new ConcurrentHashMap<>();

    private static final double CHARGING_RATE_PER_MINUTE = 1.0;

    public List<ChargingRecord> getChargingRecordsByUser(Long userId) {
        return dataStore.getChargingRecords().values().stream()
                .filter(r -> r.getUserId().equals(userId))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public ChargingRecord getChargingRecordById(Long id) {
        return dataStore.getChargingRecords().get(id);
    }

    public ChargingSession getActiveSession(Long reservationId) {
        return activeChargingSessions.get(reservationId);
    }

    public List<ChargingSession> getAllActiveSessions() {
        return new ArrayList<>(activeChargingSessions.values());
    }


    public ChargingRecord startCharging(Long reservationId) {
        Reservation reservation = dataStore.getReservations().get(reservationId);
        if (reservation == null) {
            throw new RuntimeException("预约不存在");
        }

        if (reservation.getStatus() != Reservation.ReservationStatus.CHECKED_IN) {
            throw new RuntimeException("请先签到后再开始充电");
        }

        ChargingPile pile = dataStore.getChargingPiles().get(reservation.getPileId());
        if (pile == null) {
            throw new RuntimeException("充电桩不存在");
        }

        if (pile.getStatus() == ChargingPile.PileStatus.FAULT || pile.getStatus() == ChargingPile.PileStatus.OFFLINE) {
            throw new RuntimeException("充电桩当前不可用，状态：" + pile.getStatus());
        }

        ChargingRecord record = ChargingRecord.builder()
                .id(dataStore.nextChargingRecordId())
                .recordNo(generateRecordNo())
                .reservationId(reservation.getId())
                .userId(reservation.getUserId())
                .siteId(reservation.getSiteId())
                .pileId(pile.getId())
                .startTime(LocalDateTime.now())
                .totalKwh(0.0)
                .electricityFee(BigDecimal.ZERO)
                .serviceFee(BigDecimal.ZERO)
                .totalFee(BigDecimal.ZERO)
                .status(ChargingRecord.ChargingStatus.CHARGING)
                .segments(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        dataStore.getChargingRecords().put(record.getId(), record);

        reservation.setStatus(Reservation.ReservationStatus.CHARGING);
        reservation.setUpdatedAt(LocalDateTime.now());

        pile.setStatus(ChargingPile.PileStatus.CHARGING);

        ChargingSession session = ChargingSession.builder()
                .recordId(record.getId())
                .reservationId(reservationId)
                .pileId(pile.getId())
                .userId(reservation.getUserId())
                .siteId(reservation.getSiteId())
                .startTime(record.getStartTime())
                .lastUpdateTime(record.getStartTime())
                .totalKwh(0.0)
                .currentSegment(null)
                .build();

        activeChargingSessions.put(reservationId, session);

        return record;
    }

    @Scheduled(fixedRate = 60000)
    public void updateChargingSessions() {
        LocalDateTime now = LocalDateTime.now();
        
        for (Map.Entry<Long, ChargingSession> entry : activeChargingSessions.entrySet()) {
            ChargingSession session = entry.getValue();
            
            long minutesSinceLastUpdate = java.time.Duration.between(session.getLastUpdateTime(), now).toMinutes();
            if (minutesSinceLastUpdate < 1) {
                continue;
            }

            double chargedKwh = minutesSinceLastUpdate * CHARGING_RATE_PER_MINUTE;
            
            ElectricityPriceRule currentRule = getApplicablePriceRule(session.getSiteId(), now);
            if (currentRule == null) {
                currentRule = ElectricityPriceRule.builder()
                        .period(ElectricityPriceRule.PricePeriod.FLAT)
                        .pricePerKwh(BigDecimal.valueOf(0.8))
                        .build();
            }

            ChargingSegment currentSegment = session.getCurrentSegment();
            if (currentSegment == null || !currentSegment.getPeriod().equals(currentRule.getPeriod())) {
                if (currentSegment != null) {
                    currentSegment.setEndTime(session.getLastUpdateTime());
                    BigDecimal segmentFee = currentSegment.getPricePerKwh()
                            .multiply(BigDecimal.valueOf(currentSegment.getKwh()))
                            .setScale(2, RoundingMode.HALF_UP);
                    currentSegment.setSegmentFee(segmentFee);
                    
                    ChargingRecord record = dataStore.getChargingRecords().get(session.getRecordId());
                    if (record != null) {
                        record.addSegment(currentSegment);
                    }
                }

                currentSegment = ChargingSegment.builder()
                        .id(dataStore.nextChargingRecordId())
                        .chargingRecordId(session.getRecordId())
                        .period(currentRule.getPeriod())
                        .startTime(session.getLastUpdateTime())
                        .kwh(0.0)
                        .pricePerKwh(currentRule.getPricePerKwh())
                        .build();
                session.setCurrentSegment(currentSegment);
            }

            currentSegment.setKwh(currentSegment.getKwh() + chargedKwh);
            session.setTotalKwh(session.getTotalKwh() + chargedKwh);
            session.setLastUpdateTime(now);

            ChargingRecord record = dataStore.getChargingRecords().get(session.getRecordId());
            if (record != null) {
                record.setTotalKwh(session.getTotalKwh());
                record.setUpdatedAt(now);
            }
        }
    }

    private ElectricityPriceRule getApplicablePriceRule(Long siteId, LocalDateTime time) {
        DayOfWeek dayOfWeek = time.getDayOfWeek();
        LocalTime localTime = time.toLocalTime();

        List<ElectricityPriceRule> rules = dataStore.getPriceRules().values().stream()
                .filter(r -> r.getSiteId().equals(siteId))
                .filter(r -> r.getActive())
                .filter(r -> r.getApplicableDays().contains(dayOfWeek))
                .filter(r -> !localTime.isBefore(r.getStartTime()) && localTime.isBefore(r.getEndTime()))
                .sorted(Comparator.comparingInt(ElectricityPriceRule::getPriority))
                .collect(Collectors.toList());

        return rules.isEmpty() ? null : rules.get(0);
    }


    public ChargingRecord stopCharging(Long reservationId) {
        ChargingSession session = activeChargingSessions.get(reservationId);
        if (session == null) {
            throw new RuntimeException("当前没有进行中的充电");
        }

        ChargingRecord record = dataStore.getChargingRecords().get(session.getRecordId());
        if (record == null) {
            throw new RuntimeException("充电记录不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        record.setEndTime(now);
        record.setStatus(ChargingRecord.ChargingStatus.COMPLETED);

        if (session.getCurrentSegment() != null) {
            ChargingSegment lastSegment = session.getCurrentSegment();
            lastSegment.setEndTime(now);
            BigDecimal segmentFee = lastSegment.getPricePerKwh()
                    .multiply(BigDecimal.valueOf(lastSegment.getKwh()))
                    .setScale(2, RoundingMode.HALF_UP);
            lastSegment.setSegmentFee(segmentFee);
            record.addSegment(lastSegment);
        }

        calculateChargingFees(record);

        SettlementResult settlementResult = settleCharging(record, reservationId);

        Reservation reservation = dataStore.getReservations().get(reservationId);
        if (reservation != null) {
            reservation.setStatus(settlementResult.isOverdue ? 
                    Reservation.ReservationStatus.COMPLETED : 
                    Reservation.ReservationStatus.COMPLETED);
            reservation.setCheckOutTime(now);
            reservation.setUpdatedAt(now);
        }

        ChargingPile pile = dataStore.getChargingPiles().get(session.getPileId());
        if (pile != null) {
            pile.setStatus(ChargingPile.PileStatus.IDLE);
        }

        activeChargingSessions.remove(reservationId);

        record.setUpdatedAt(now);

        return record;
    }


    public AbnormalOrder handlePileFault(Long reservationId, String description) {
        ChargingSession session = activeChargingSessions.get(reservationId);
        if (session == null) {
            throw new RuntimeException("当前没有进行中的充电");
        }

        ChargingRecord record = dataStore.getChargingRecords().get(session.getRecordId());
        if (record == null) {
            throw new RuntimeException("充电记录不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        record.setEndTime(now);
        record.setStatus(ChargingRecord.ChargingStatus.ABNORMAL_STOPPED);

        double totalKwh = 0.0;
        if (session.getCurrentSegment() != null) {
            ChargingSegment lastSegment = session.getCurrentSegment();
            lastSegment.setEndTime(now);
            totalKwh = lastSegment.getKwh();
            record.addSegment(lastSegment);
        }
        record.setTotalKwh(totalKwh);

        Reservation reservation = dataStore.getReservations().get(reservationId);
        if (reservation != null) {
            reservation.setStatus(Reservation.ReservationStatus.ABNORMAL);
            reservation.setUpdatedAt(now);
        }

        ChargingPile pile = dataStore.getChargingPiles().get(session.getPileId());
        if (pile != null) {
            pile.setStatus(ChargingPile.PileStatus.FAULT);
        }

        AbnormalOrder abnormalOrder = AbnormalOrder.builder()
                .id(dataStore.nextAbnormalOrderId())
                .abnormalNo(generateAbnormalNo())
                .reservationId(reservationId)
                .chargingRecordId(record.getId())
                .userId(session.getUserId())
                .siteId(session.getSiteId())
                .pileId(session.getPileId())
                .type(AbnormalOrder.AbnormalType.PILE_FAULT)
                .status(AbnormalOrder.AbnormalStatus.PENDING)
                .occurTime(now)
                .chargedKwh(totalKwh)
                .description(description != null ? description : "充电桩故障")
                .createdAt(now)
                .updatedAt(now)
                .build();

        dataStore.getAbnormalOrders().put(abnormalOrder.getId(), abnormalOrder);

        User user = dataStore.getUsers().get(session.getUserId());
        if (user != null && reservation != null) {
            BigDecimal depositAmount = reservation.getDepositAmount();
            if (depositAmount != null && depositAmount.compareTo(BigDecimal.ZERO) > 0) {
                user.setFrozenAmount(user.getFrozenAmount().subtract(depositAmount));
                user.setBalance(user.getBalance().add(depositAmount));
            }
        }

        activeChargingSessions.remove(reservationId);

        record.setUpdatedAt(now);

        return abnormalOrder;
    }

    private void calculateChargingFees(ChargingRecord record) {
        BigDecimal totalElectricityFee = BigDecimal.ZERO;
        double totalKwh = 0.0;

        if (record.getSegments() != null) {
            for (ChargingSegment segment : record.getSegments()) {
                if (segment.getSegmentFee() != null) {
                    totalElectricityFee = totalElectricityFee.add(segment.getSegmentFee());
                }
                if (segment.getKwh() != null) {
                    totalKwh += segment.getKwh();
                }
            }
        }

        Site site = dataStore.getSites().get(record.getSiteId());
        BigDecimal serviceFeeRate = site != null ? 
                BigDecimal.valueOf(site.getServiceFeePerKwh()) : 
                BigDecimal.valueOf(0.6);
        
        BigDecimal totalServiceFee = serviceFeeRate.multiply(BigDecimal.valueOf(totalKwh))
                .setScale(2, RoundingMode.HALF_UP);

        record.setTotalKwh(totalKwh);
        record.setElectricityFee(totalElectricityFee);
        record.setServiceFee(totalServiceFee);
        record.setTotalFee(totalElectricityFee.add(totalServiceFee));
    }

    private SettlementResult settleCharging(ChargingRecord record, Long reservationId) {
        Reservation reservation = dataStore.getReservations().get(reservationId);
        User user = dataStore.getUsers().get(record.getUserId());
        
        if (reservation == null || user == null) {
            return new SettlementResult(false);
        }

        BigDecimal totalFee = record.getTotalFee();
        BigDecimal depositAmount = reservation.getDepositAmount();
        
        BigDecimal availableFrozen = depositAmount != null ? depositAmount : BigDecimal.ZERO;
        
        BigDecimal remainingFrozen = user.getFrozenAmount().subtract(availableFrozen);
        user.setFrozenAmount(remainingFrozen);

        if (totalFee.compareTo(BigDecimal.ZERO) <= 0) {
            if (availableFrozen.compareTo(BigDecimal.ZERO) > 0) {
                user.setBalance(user.getBalance().add(availableFrozen));
            }
            return new SettlementResult(false);
        }

        if (availableFrozen.compareTo(totalFee) >= 0) {
            BigDecimal refund = availableFrozen.subtract(totalFee);
            if (refund.compareTo(BigDecimal.ZERO) > 0) {
                user.setBalance(user.getBalance().add(refund));
            }
        } else {
            BigDecimal balanceBefore = user.getBalance();
            BigDecimal amountToPay = totalFee.subtract(availableFrozen);
            
            if (balanceBefore.compareTo(amountToPay) >= 0) {
                user.setBalance(balanceBefore.subtract(amountToPay));
            } else {
                user.setBalance(BigDecimal.ZERO);
                user.setOverdueCount(user.getOverdueCount() + 1);
                BigDecimal overdueAmount = amountToPay.subtract(balanceBefore);
                record.setRemark("欠费" + overdueAmount + "元");
                updateUserCreditStatus(user);
                return new SettlementResult(true);
            }
        }

        updateUserCreditStatus(user);

        return new SettlementResult(false);
    }

    private void updateUserCreditStatus(User user) {
        int overdueCount = user.getOverdueCount();
        if (overdueCount >= 5) {
            user.setCreditStatus(User.CreditStatus.BLACKLIST);
        } else if (overdueCount >= 3) {
            user.setCreditStatus(User.CreditStatus.POOR);
        } else if (overdueCount >= 2) {
            user.setCreditStatus(User.CreditStatus.NORMAL);
        } else if (overdueCount >= 1) {
            user.setCreditStatus(User.CreditStatus.GOOD);
        }
    }


    public AbnormalOrder resolveAbnormalOrder(Long abnormalOrderId, boolean compensate, String remark) {
        AbnormalOrder abnormalOrder = dataStore.getAbnormalOrders().get(abnormalOrderId);
        if (abnormalOrder == null) {
            throw new RuntimeException("异常单不存在");
        }

        if (abnormalOrder.getStatus() == AbnormalOrder.AbnormalStatus.COMPENSATED ||
            abnormalOrder.getStatus() == AbnormalOrder.AbnormalStatus.RESOLVED) {
            throw new RuntimeException("异常单已处理");
        }

        LocalDateTime now = LocalDateTime.now();
        abnormalOrder.setResolveTime(now);
        abnormalOrder.setRemark(remark);
        abnormalOrder.setUpdatedAt(now);

        if (compensate) {
            ChargingRecord record = dataStore.getChargingRecords().get(abnormalOrder.getChargingRecordId());
            if (record != null) {
                BigDecimal compensation;
                
                if (record.getElectricityFee() != null && record.getElectricityFee().compareTo(BigDecimal.ZERO) > 0) {
                    compensation = record.getElectricityFee().multiply(BigDecimal.valueOf(0.5))
                            .setScale(2, RoundingMode.HALF_UP);
                } else if (abnormalOrder.getChargedKwh() != null && abnormalOrder.getChargedKwh() > 0) {
                    PriceRule rule = findApplicablePriceRule(abnormalOrder.getSiteId(), abnormalOrder.getOccurTime());
                    BigDecimal pricePerKwh = rule != null ? rule.getPricePerKwh() : BigDecimal.valueOf(0.8);
                    compensation = pricePerKwh.multiply(BigDecimal.valueOf(abnormalOrder.getChargedKwh()))
                            .multiply(BigDecimal.valueOf(0.5))
                            .setScale(2, RoundingMode.HALF_UP);
                } else {
                    compensation = BigDecimal.ZERO;
                }
                
                abnormalOrder.setCompensatedAmount(compensation);
                abnormalOrder.setStatus(AbnormalOrder.AbnormalStatus.COMPENSATED);

                if (compensation.compareTo(BigDecimal.ZERO) > 0) {
                    User user = dataStore.getUsers().get(abnormalOrder.getUserId());
                    if (user != null) {
                        user.setBalance(user.getBalance().add(compensation));
                    }
                }
            }
        } else {
            abnormalOrder.setStatus(AbnormalOrder.AbnormalStatus.RESOLVED);
        }

        ChargingPile pile = dataStore.getChargingPiles().get(abnormalOrder.getPileId());
        if (pile != null && pile.getStatus() == ChargingPile.PileStatus.FAULT) {
            pile.setStatus(ChargingPile.PileStatus.IDLE);
        }

        return abnormalOrder;
    }

    private String generateRecordNo() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        String uuid = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "REC" + timestamp + uuid;
    }

    private String generateAbnormalNo() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        String uuid = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "ABN" + timestamp + uuid;
    }

    private static class SettlementResult {
        boolean isOverdue;

        SettlementResult(boolean isOverdue) {
            this.isOverdue = isOverdue;
        }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ChargingSession {
        private Long recordId;
        private Long reservationId;
        private Long pileId;
        private Long userId;
        private Long siteId;
        private LocalDateTime startTime;
        private LocalDateTime lastUpdateTime;
        private Double totalKwh;
        private ChargingSegment currentSegment;
    }
}
