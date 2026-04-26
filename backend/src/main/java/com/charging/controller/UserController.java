package com.charging.controller;

import com.charging.dto.ApiResponse;
import com.charging.entity.User;
import com.charging.entity.Vehicle;
import com.charging.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ApiResponse<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ApiResponse.success(users);
    }

    @GetMapping("/{id}")
    public ApiResponse<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return ApiResponse.error("用户不存在");
        }
        return ApiResponse.success(user);
    }

    @PostMapping("/{userId}/recharge")
    public ApiResponse<User> recharge(@PathVariable Long userId, @RequestParam BigDecimal amount) {
        try {
            User user = userService.recharge(userId, amount);
            return ApiResponse.success("充值成功", user);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/{userId}/vehicles")
    public ApiResponse<List<Vehicle>> getVehiclesByUser(@PathVariable Long userId) {
        List<Vehicle> vehicles = userService.getVehiclesByUser(userId);
        return ApiResponse.success(vehicles);
    }

    @PostMapping("/{userId}/vehicles")
    public ApiResponse<Vehicle> addVehicle(@PathVariable Long userId, @RequestBody Vehicle vehicle) {
        try {
            Vehicle saved = userService.addVehicle(userId, vehicle);
            return ApiResponse.success("添加车辆成功", saved);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @DeleteMapping("/{userId}/vehicles/{vehicleId}")
    public ApiResponse<Vehicle> deleteVehicle(@PathVariable Long userId, @PathVariable Long vehicleId) {
        try {
            Vehicle vehicle = userService.deleteVehicle(userId, vehicleId);
            return ApiResponse.success("删除车辆成功", vehicle);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
