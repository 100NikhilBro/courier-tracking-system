package com.minelsaygisever.couriertrackingsystem.controller;

import com.minelsaygisever.couriertrackingsystem.dto.admin.AdminResponse;
import com.minelsaygisever.couriertrackingsystem.dto.admin.CreateAdminRequest;
import com.minelsaygisever.couriertrackingsystem.dto.admin.UpdateAdminRequest;
import com.minelsaygisever.couriertrackingsystem.service.AdminManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admins")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Management", description = "APIs for managing admin users")
public class AdminManagementController {
    private final AdminManagementService adminManagementService;

    @PostMapping
    @Operation(
            summary = "Create a new Admin",
            description = "Creates a new admin user with the provided details. Username must be unique."
    )
    public AdminResponse createAdmin(@Valid @RequestBody CreateAdminRequest request) {
        return adminManagementService.createAdmin(request);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get Admin by User ID", description = "Retrieves detailed information of a specific admin.")
    public AdminResponse getAdmin(@PathVariable Long userId) {
        return adminManagementService.getAdminByUserId(userId);
    }

    @GetMapping("/username/{username}")
    @Operation(summary = "Get Admin by Username", description = "Retrieves admin details using their username.")
    public AdminResponse getAdminByUsername(@PathVariable String username) {
        return adminManagementService.getAdminByUsername(username);
    }

    @GetMapping
    @Operation(summary = "List all Admins", description = "Returns a paginated list of all admin users.")
    public Page<AdminResponse> getAdmins(Pageable pageable) {
        return adminManagementService.getAdmins(pageable);
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update Admin", description = "Updates an existing admin's information.")    public AdminResponse updateAdmin(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateAdminRequest request
    ) {
        return adminManagementService.updateAdmin(userId, request);
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete Admin", description = "Deletes an admin user and their associated profile.")
    public void deleteAdmin(@PathVariable Long userId) {
        adminManagementService.deleteAdmin(userId);
    }
}
