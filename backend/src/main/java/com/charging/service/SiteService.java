package com.charging.service;

import com.charging.entity.*;
import com.charging.repository.DataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SiteService {

    @Autowired
    private DataStore dataStore;

    public List<Site> getAllSites() {
        return dataStore.getSites().values().stream()
                .filter(Site::getActive)
                .collect(Collectors.toList());
    }

    public Site getSiteById(Long id) {
        return dataStore.getSites().get(id);
    }

    public List<ChargingPile> getPilesBySite(Long siteId) {
        return dataStore.getChargingPiles().values().stream()
                .filter(p -> p.getSiteId().equals(siteId))
                .filter(ChargingPile::getActive)
                .collect(Collectors.toList());
    }

    public ChargingPile getPileById(Long id) {
        return dataStore.getChargingPiles().get(id);
    }

    public List<ElectricityPriceRule> getPriceRulesBySite(Long siteId) {
        return dataStore.getPriceRules().values().stream()
                .filter(r -> r.getSiteId().equals(siteId))
                .filter(ElectricityPriceRule::getActive)
                .sorted(Comparator.comparingInt(ElectricityPriceRule::getPriority))
                .collect(Collectors.toList());
    }

    public List<ElectricityPriceRule> getCurrentPriceRules(Long siteId) {
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek dayOfWeek = now.getDayOfWeek();
        LocalTime localTime = now.toLocalTime();

        return dataStore.getPriceRules().values().stream()
                .filter(r -> r.getSiteId().equals(siteId))
                .filter(ElectricityPriceRule::getActive)
                .filter(r -> r.getApplicableDays().contains(dayOfWeek))
                .filter(r -> !localTime.isBefore(r.getStartTime()) && localTime.isBefore(r.getEndTime()))
                .sorted(Comparator.comparingInt(ElectricityPriceRule::getPriority))
                .collect(Collectors.toList());
    }

    public boolean isPileAvailable(Long pileId, LocalDateTime startTime, LocalDateTime endTime) {
        ChargingPile pile = dataStore.getChargingPiles().get(pileId);
        if (pile == null || !pile.getActive()) {
            return false;
        }

        if (pile.getStatus() != ChargingPile.PileStatus.IDLE) {
            return false;
        }

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

        return !hasConflict;
    }
}
