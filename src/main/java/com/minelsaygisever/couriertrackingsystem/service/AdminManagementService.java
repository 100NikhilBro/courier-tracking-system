package com.minelsaygisever.couriertrackingsystem.service;


import com.minelsaygisever.couriertrackingsystem.domain.AdminProfile;
import com.minelsaygisever.couriertrackingsystem.domain.Role;
import com.minelsaygisever.couriertrackingsystem.domain.User;
import com.minelsaygisever.couriertrackingsystem.dto.admin.AdminResponse;
import com.minelsaygisever.couriertrackingsystem.dto.admin.CreateAdminRequest;
import com.minelsaygisever.couriertrackingsystem.dto.admin.UpdateAdminRequest;
import com.minelsaygisever.couriertrackingsystem.exception.ResourceNotFoundException;
import com.minelsaygisever.couriertrackingsystem.exception.UsernameAlreadyExistsException;
import com.minelsaygisever.couriertrackingsystem.repository.AdminProfileRepository;
import com.minelsaygisever.couriertrackingsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminManagementService {
    private final UserRepository userRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AdminResponse createAdmin(CreateAdminRequest request) {
        userRepository.findByUsername(request.getUsername())
                .ifPresent(u -> {
                    throw new UsernameAlreadyExistsException("Username already exists: " + request.getUsername());
                });

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_ADMIN)
                .isEnabled(true)
                .build();

        AdminProfile profile = AdminProfile.builder()
                .user(user)
                .fullName(request.getFullName())
                .build();

        adminProfileRepository.save(profile);

        return mapToResponse(profile);
    }

    @Transactional(readOnly = true)
    public AdminResponse getAdminByUserId(Long userId) {
        AdminProfile profile = adminProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin profile not found for User ID: " + userId));

        return mapToResponse(profile);
    }

    @Transactional(readOnly = true)
    public AdminResponse getAdminByUsername(String username) {
        AdminProfile profile = adminProfileRepository.findByUser_Username(username)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with username: " + username));

        return mapToResponse(profile);
    }

    @Transactional(readOnly = true)
    public Page<AdminResponse> getAdmins(Pageable pageable) {
        return adminProfileRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public AdminResponse updateAdmin(Long userId, UpdateAdminRequest request) {
        AdminProfile profile = adminProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin profile not found for User ID: " + userId));

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

        adminProfileRepository.save(profile);

        return mapToResponse(profile);
    }

    @Transactional
    public void deleteAdmin(Long userId) {
        AdminProfile profile = adminProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin profile not found for User ID: " + userId));

        User user = profile.getUser();

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (user.getUsername().equals(currentUsername)) {
            throw new IllegalStateException("You cannot delete your own account!");
        }

        user.setEnabled(false);

        log.info("Admin user {} deactivated (Soft Delete).", userId);

        userRepository.save(user);
    }

    private AdminResponse mapToResponse(AdminProfile profile) {
        User user = profile.getUser();

        return AdminResponse.builder()
                .id(profile.getId())
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(profile.getFullName())
                .enabled(user.isEnabled())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
