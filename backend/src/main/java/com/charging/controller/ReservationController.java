package com.charging.controller;

import com.charging.dto.ApiResponse;
import com.charging.dto.ReservationRequest;
import com.charging.entity.Reservation;
import com.charging.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@CrossOrigin(origins = "*")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @GetMapping("/user/{userId}")
    public ApiResponse<List<Reservation>> getReservationsByUser(@PathVariable Long userId) {
        List<Reservation> reservations = reservationService.getReservationsByUser(userId);
        return ApiResponse.success(reservations);
    }

    @GetMapping("/{id}")
    public ApiResponse<Reservation> getReservationById(@PathVariable Long id) {
        Reservation reservation = reservationService.getReservationById(id);
        if (reservation == null) {
            return ApiResponse.error("预约不存在");
        }
        return ApiResponse.success(reservation);
    }

    @GetMapping("/active")
    public ApiResponse<List<Reservation>> getActiveReservations() {
        List<Reservation> reservations = reservationService.getActiveReservations();
        return ApiResponse.success(reservations);
    }

    @PostMapping
    public ApiResponse<Reservation> createReservation(@Valid @RequestBody ReservationRequest request) {
        try {
            Reservation reservation = reservationService.createReservation(request);
            return ApiResponse.success("预约成功", reservation);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/{reservationId}/cancel")
    public ApiResponse<Reservation> cancelReservation(
            @PathVariable Long reservationId,
            @RequestParam Long userId) {
        try {
            Reservation reservation = reservationService.cancelReservation(reservationId, userId);
            return ApiResponse.success("取消预约成功", reservation);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/{reservationId}/checkin")
    public ApiResponse<Reservation> checkIn(@PathVariable Long reservationId) {
        try {
            Reservation reservation = reservationService.checkIn(reservationId);
            return ApiResponse.success("签到成功", reservation);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
