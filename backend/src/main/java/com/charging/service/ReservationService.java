package com.charging.service;

import com.charging.dto.ReservationRequest;
import com.charging.entity.*;
import com.charging.repository.DataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    @Autowired
    private DataStore dataStore;

    private static final BigDecimal DEPOSIT_AMOUNT = BigDecimal.valueOf(50.00);
    private static final int RESERVATION_TIMEOUT_MINUTES = 15;

    public List<Reservation> getReservationsByUser(Long userId) {
        return dataStore.getReservations().values().stream()
                .filter(r -> r.getUserId().equals(userId))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public Reservation getReservationById(Long id) {
        return dataStore.getReservations().get(id);
    }

    public List<Reservation> getActiveReservations() {
        return dataStore.getReservations().values().stream()
                .filter(r -> r.getStatus() == Reservation.ReservationStatus.RESERVED)
                .collect(Collectors.toList());
    }

    public Reservation createReservation(ReservationRequest request) {
        User user = dataStore.getUsers().get(request.getUserId());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (user.getCreditStatus() == User.CreditStatus.BLACKLIST) {
            throw new RuntimeException("用户已被拉黑，无法预约");
        }

        Vehicle vehicle = dataStore.getVehicles().get(request.getVehicleId());
        if (vehicle == null || !vehicle.getUserId().equals(user.getId())) {
            throw new RuntimeException("车辆不存在或不属于当前用户");
        }

        ChargingPile pile = dataStore.getChargingPiles().get(request.getPileId());
        if (pile == null) {
            throw new RuntimeException("充电桩不存在");
        }

        if (pile.getStatus() != ChargingPile.PileStatus.IDLE) {
            throw new RuntimeException("充电桩当前不可预约，状态：" + pile.getStatus());
        }

        Site site = dataStore.getSites().get(pile.getSiteId());
        if (site == null) {
            throw new RuntimeException("站点不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        if (request.getStartTime().isBefore(now)) {
            throw new RuntimeException("预约开始时间不能早于当前时间");
        }

        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new RuntimeException("预约结束时间不能早于开始时间");
        }

        long minutes = java.time.Duration.between(request.getStartTime(), request.getEndTime()).toMinutes();
        if (minutes < 30) {
            throw new RuntimeException("预约时长至少为30分钟");
        }

        if (minutes > 480) {
            throw new RuntimeException("预约时长最多为8小时");
        }

        BigDecimal totalBalance = user.getBalance().subtract(user.getFrozenAmount());
        if (totalBalance.compareTo(DEPOSIT_AMOUNT) < 0) {
            throw new RuntimeException("账户余额不足，预约需要冻结" + DEPOSIT_AMOUNT + "元保证金");
        }

        checkTimeConflict(pile.getId(), request.getStartTime(), request.getEndTime());

        Reservation reservation = Reservation.builder()
                .id(dataStore.nextReservationId())
                .reservationNo(generateReservationNo())
                .userId(user.getId())
                .vehicleId(vehicle.getId())
                .siteId(site.getId())
                .pileId(pile.getId())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .expectedCheckInTime(request.getStartTime())
                .status(Reservation.ReservationStatus.RESERVED)
                .depositAmount(DEPOSIT_AMOUNT)
                .depositDeducted(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .remark(request.getRemark())
                .build();

        dataStore.getReservations().put(reservation.getId(), reservation);

        user.setFrozenAmount(user.getFrozenAmount().add(DEPOSIT_AMOUNT));

        pile.setStatus(ChargingPile.PileStatus.RESERVED);

        return reservation;
    }

    private void checkTimeConflict(Long pileId, LocalDateTime startTime, LocalDateTime endTime) {
        boolean hasConflict = dataStore.getReservations().values().stream()
                .filter(r -> r.getPileId().equals(pileId))
                .filter(r -> r.getStatus() == Reservation.ReservationStatus.RESERVED
                        || r.getStatus() == Reservation.ReservationStatus.CHECKED_IN
                        || r.getStatus() == Reservation.ReservationStatus.CHARGING)
                .anyMatch(r -> {
                    LocalDateTime rStart = r.getStartTime();
                    LocalDateTime rEnd = r.getEndTime();
                    return !(endTime.isBefore(rStart) || startTime.isAfter(rEnd));
                });

        if (hasConflict) {
            throw new RuntimeException("该时间段已被预约，请选择其他时间");
        }
    }

    public Reservation cancelReservation(Long reservationId, Long userId) {
        Reservation reservation = dataStore.getReservations().get(reservationId);
        if (reservation == null) {
            throw new RuntimeException("预约不存在");
        }

        if (!reservation.getUserId().equals(userId)) {
            throw new RuntimeException("无权取消该预约");
        }

        if (reservation.getStatus() != Reservation.ReservationStatus.RESERVED) {
            throw new RuntimeException("当前预约状态无法取消");
        }

        LocalDateTime now = LocalDateTime.now();
        long minutesBeforeStart = java.time.Duration.between(now, reservation.getStartTime()).toMinutes();

        if (minutesBeforeStart < 30) {
            BigDecimal deductedAmount = DEPOSIT_AMOUNT.multiply(BigDecimal.valueOf(0.5));
            reservation.setDepositDeducted(deductedAmount);

            User user = dataStore.getUsers().get(userId);
            user.setFrozenAmount(user.getFrozenAmount().subtract(DEPOSIT_AMOUNT));
            user.setBalance(user.getBalance().subtract(deductedAmount));

            reservation.setStatus(Reservation.ReservationStatus.CANCELLED);
            reservation.setRemark("预约开始前30分钟内取消，扣除50%保证金");
        } else {
            User user = dataStore.getUsers().get(userId);
            user.setFrozenAmount(user.getFrozenAmount().subtract(DEPOSIT_AMOUNT));

            reservation.setStatus(Reservation.ReservationStatus.CANCELLED);
            reservation.setDepositDeducted(BigDecimal.ZERO);
            reservation.setRemark("用户主动取消预约");
        }

        reservation.setUpdatedAt(LocalDateTime.now());

        ChargingPile pile = dataStore.getChargingPiles().get(reservation.getPileId());
        pile.setStatus(ChargingPile.PileStatus.IDLE);

        return reservation;
    }

    public Reservation checkIn(Long reservationId) {
        Reservation reservation = dataStore.getReservations().get(reservationId);
        if (reservation == null) {
            throw new RuntimeException("预约不存在");
        }

        if (reservation.getStatus() != Reservation.ReservationStatus.RESERVED) {
            throw new RuntimeException("当前预约状态无法签到");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime latestCheckInTime = reservation.getExpectedCheckInTime().plusMinutes(RESERVATION_TIMEOUT_MINUTES);

        if (now.isAfter(latestCheckInTime)) {
            throw new RuntimeException("已超过签到时间，预约已取消");
        }

        reservation.setStatus(Reservation.ReservationStatus.CHECKED_IN);
        reservation.setActualCheckInTime(now);
        reservation.setUpdatedAt(now);

        return reservation;
    }

    public void processTimeoutReservation(Reservation reservation) {
        if (reservation.getStatus() != Reservation.ReservationStatus.RESERVED) {
            return;
        }

        LocalDateTime latestCheckInTime = reservation.getExpectedCheckInTime().plusMinutes(RESERVATION_TIMEOUT_MINUTES);
        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(latestCheckInTime)) {
            reservation.setStatus(Reservation.ReservationStatus.TIMEOUT_CANCELLED);
            reservation.setDepositDeducted(DEPOSIT_AMOUNT.multiply(BigDecimal.valueOf(0.3)));
            reservation.setUpdatedAt(now);
            reservation.setRemark("超时未签到，预约取消，扣除30%保证金");

            User user = dataStore.getUsers().get(reservation.getUserId());
            user.setFrozenAmount(user.getFrozenAmount().subtract(DEPOSIT_AMOUNT));
            user.setBalance(user.getBalance().subtract(reservation.getDepositDeducted()));
            user.setOverdueCount(user.getOverdueCount() + 1);

            updateUserCreditStatus(user);

            ChargingPile pile = dataStore.getChargingPiles().get(reservation.getPileId());
            pile.setStatus(ChargingPile.PileStatus.IDLE);
        }
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

    private String generateReservationNo() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        String uuid = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "RES" + timestamp + uuid;
    }
}
