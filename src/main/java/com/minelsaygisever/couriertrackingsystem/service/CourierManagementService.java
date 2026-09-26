package com.minelsaygisever.couriertrackingsystem.service;

import com.minelsaygisever.couriertrackingsystem.domain.CourierProfile;
import com.minelsaygisever.couriertrackingsystem.domain.Role;
import com.minelsaygisever.couriertrackingsystem.domain.User;
import com.minelsaygisever.couriertrackingsystem.dto.courier.CourierResponse;
import com.minelsaygisever.couriertrackingsystem.dto.courier.CreateCourierRequest;
import com.minelsaygisever.couriertrackingsystem.dto.courier.UpdateCourierRequest;
import com.minelsaygisever.couriertrackingsystem.exception.ResourceNotFoundException;
import com.minelsaygisever.couriertrackingsystem.exception.UsernameAlreadyExistsException;
import com.minelsaygisever.couriertrackingsystem.repository.CourierProfileRepository;
import com.minelsaygisever.couriertrackingsystem.repository.UserRepository;
import com.minelsaygisever.couriertrackingsystem.util.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourierManagementService {
    private final UserRepository userRepository;
    private final CourierProfileRepository courierProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional(readOnly = true)
    public Double getTotalDistance(Long userId) {
        CourierProfile profile = courierProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Courier profile not found for User ID: " + userId));

        double dbDistance = profile.getTotalDistanceInMeters();

        Double bufferDistance = 0.0;

        Object bufferVal = redisTemplate.opsForHash().get(RedisKeys.DISTANCE_BUFFER_KEY, String.valueOf(userId));

        if (bufferVal != null) {
            bufferDistance = ((Number) bufferVal).doubleValue();
        }

        return dbDistance + bufferDistance;
    }

    @Transactional
    public CourierResponse createCourier(CreateCourierRequest request) {
        userRepository.findByUsername(request.getUsername())
                .ifPresent(u -> {
                    throw new UsernameAlreadyExistsException("Username already exists: " + request.getUsername());
                });

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_COURIER)
                .isEnabled(true)
                .build();

        CourierProfile profile = CourierProfile.builder()
                .user(user)
                .fullName(request.getFullName())
                .totalDistanceInMeters(0.0)
                .build();

        courierProfileRepository.save(profile);

        return mapToResponse(profile);
    }


    @Transactional(readOnly = true)
    public CourierResponse getCourierByUserId(Long userId) {
        CourierProfile profile = courierProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Courier profile not found for User ID: " + userId));

        return mapToResponse(profile);
    }

    @Transactional(readOnly = true)
    public Page<CourierResponse> getCouriers(Pageable pageable) {
        return courierProfileRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public CourierResponse getCourierByUsername(String username) {
        CourierProfile profile = courierProfileRepository.findByUser_Username(username)
                .orElseThrow(() -> new ResourceNotFoundException("Courier not found with username: " + username));

        return mapToResponse(profile);
    }

    @Transactional
    @CacheEvict(value = "courier-validity", key = "#userId")
    public CourierResponse updateCourier(Long userId, UpdateCourierRequest request) {
        CourierProfile profile = courierProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Courier profile not found for User ID: " + userId));

        User user = profile.getUser();

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            profile.setFullName(request.getFullName());
        }

        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        courierProfileRepository.save(profile);

        return mapToResponse(profile);
    }


    @Transactional
    @CacheEvict(value = "courier-validity", key = "#userId")
    public void deleteCourier(Long userId) {
        CourierProfile profile = courierProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Courier profile not found for User ID: " + userId));

        User user = profile.getUser();

        user.setEnabled(false);

        log.info("Courier user {} deactivated (Soft Delete). Profile and logs are preserved.", userId);

        userRepository.save(user);
    }

    private CourierResponse mapToResponse(CourierProfile profile) {
        User user = profile.getUser();

        return CourierResponse.builder()
                .id(profile.getId())
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(profile.getFullName())
                .enabled(user.isEnabled())
                .totalDistanceInMeters(profile.getTotalDistanceInMeters())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    @Cacheable(value = "courier-validity", key = "#userId", unless = "#result == false")
    public boolean isActiveCourier(Long userId) {
        return userRepository.findById(userId)
                .map(user -> {
                    boolean isValid = user.getRole() == Role.ROLE_COURIER && user.isEnabled();
                    if (!isValid) {
                        log.warn("Location received for user {} but they are NOT an active courier. Role: {}", userId, user.getRole());
                    }
                    return isValid;
                })
                .orElseGet(() -> {
                    log.warn("Location received for unknown user ID: {}", userId);
                    return false;
                });
    }

}
