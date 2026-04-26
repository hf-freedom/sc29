package com.charging.controller;

import com.charging.dto.ApiResponse;
import com.charging.entity.*;
import com.charging.service.SiteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/sites")
@CrossOrigin(origins = "*")
public class SiteController {

    @Autowired
    private SiteService siteService;

    @GetMapping
    public ApiResponse<List<Site>> getAllSites() {
        List<Site> sites = siteService.getAllSites();
        return ApiResponse.success(sites);
    }

    @GetMapping("/{id}")
    public ApiResponse<Site> getSiteById(@PathVariable Long id) {
        Site site = siteService.getSiteById(id);
        if (site == null) {
            return ApiResponse.error("站点不存在");
        }
        return ApiResponse.success(site);
    }

    @GetMapping("/{siteId}/piles")
    public ApiResponse<List<ChargingPile>> getPilesBySite(@PathVariable Long siteId) {
        List<ChargingPile> piles = siteService.getPilesBySite(siteId);
        return ApiResponse.success(piles);
    }

    @GetMapping("/piles/{pileId}")
    public ApiResponse<ChargingPile> getPileById(@PathVariable Long pileId) {
        ChargingPile pile = siteService.getPileById(pileId);
        if (pile == null) {
            return ApiResponse.error("充电桩不存在");
        }
        return ApiResponse.success(pile);
    }

    @GetMapping("/{siteId}/price-rules")
    public ApiResponse<List<ElectricityPriceRule>> getPriceRulesBySite(@PathVariable Long siteId) {
        List<ElectricityPriceRule> rules = siteService.getPriceRulesBySite(siteId);
        return ApiResponse.success(rules);
    }

    @GetMapping("/{siteId}/current-price")
    public ApiResponse<List<ElectricityPriceRule>> getCurrentPriceRules(@PathVariable Long siteId) {
        List<ElectricityPriceRule> rules = siteService.getCurrentPriceRules(siteId);
        return ApiResponse.success(rules);
    }

    @GetMapping("/piles/{pileId}/availability")
    public ApiResponse<Boolean> checkPileAvailability(
            @PathVariable Long pileId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        boolean available = siteService.isPileAvailable(pileId, startTime, endTime);
        return ApiResponse.success(available);
    }
}
