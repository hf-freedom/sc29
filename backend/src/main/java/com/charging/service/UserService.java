package com.charging.service;

import com.charging.entity.User;
import com.charging.entity.Vehicle;
import com.charging.repository.DataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private DataStore dataStore;

    public User getUserById(Long id) {
        return dataStore.getUsers().get(id);
    }

    public List<User> getAllUsers() {
        return dataStore.getUsers().values().stream()
                .collect(Collectors.toList());
    }


    public User recharge(Long userId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("充值金额必须大于0");
        }

        User user = dataStore.getUsers().get(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        user.setBalance(user.getBalance().add(amount));
        return user;
    }


    public Vehicle addVehicle(Long userId, Vehicle vehicle) {
        User user = dataStore.getUsers().get(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        vehicle.setId(dataStore.nextVehicleId());
        vehicle.setUserId(userId);
        vehicle.setActive(true);

        dataStore.getVehicles().put(vehicle.getId(), vehicle);
        return vehicle;
    }

    public List<Vehicle> getVehiclesByUser(Long userId) {
        return dataStore.getVehicles().values().stream()
                .filter(v -> v.getUserId().equals(userId))
                .filter(Vehicle::getActive)
                .collect(Collectors.toList());
    }

    public Vehicle getVehicleById(Long id) {
        return dataStore.getVehicles().get(id);
    }


    public Vehicle deleteVehicle(Long userId, Long vehicleId) {
        Vehicle vehicle = dataStore.getVehicles().get(vehicleId);
        if (vehicle == null) {
            throw new RuntimeException("车辆不存在");
        }

        if (!vehicle.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作该车辆");
        }

        vehicle.setActive(false);
        return vehicle;
    }
}
