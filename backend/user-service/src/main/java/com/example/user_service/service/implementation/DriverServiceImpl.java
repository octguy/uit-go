package com.example.user_service.service.implementation;

import com.example.user_service.dto.request.RegisterDriverRequest;
import com.example.user_service.dto.response.DriverResponse;
import com.example.user_service.entity.Driver;
import com.example.user_service.entity.User;
import com.example.user_service.enums.UserRole;
import com.example.user_service.repository.DriverRepository;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.service.IDriverService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DriverServiceImpl implements IDriverService {

    private final DriverRepository driverRepository;

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    public DriverServiceImpl(DriverRepository driverRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.driverRepository = driverRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public DriverResponse createDriver(RegisterDriverRequest request) {
        User user = new User();
        Driver driver = new Driver();

        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.ROLE_DRIVER);

        driver.setVehicleModel(request.getVehicleModel());
        driver.setVehicleNumber(request.getVehicleNumber());

        userRepository.save(user);
        driver.setUser(user);
        driverRepository.save(driver);

        return DriverResponse.builder()
                .id(driver.getId())
                .email(request.getEmail())
                .vehicleModel(driver.getVehicleModel())
                .vehicleNumber(driver.getVehicleNumber())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Override
    public List<DriverResponse> getAllDrivers() {
        List<Driver> drivers = driverRepository.findAll();
        return drivers.stream()
                .map(driver -> DriverResponse.builder()
                        .id(driver.getId())
                        .email(driver.getUser().getEmail())
                        .vehicleModel(driver.getVehicleModel())
                        .vehicleNumber(driver.getVehicleNumber())
                        .createdAt(driver.getCreatedAt())
                        .build())
                .toList();
    }
}
