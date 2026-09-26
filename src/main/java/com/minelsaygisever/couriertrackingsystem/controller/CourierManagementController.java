package com.minelsaygisever.couriertrackingsystem.controller;

import com.minelsaygisever.couriertrackingsystem.dto.courier.CourierResponse;
import com.minelsaygisever.couriertrackingsystem.dto.courier.CreateCourierRequest;
import com.minelsaygisever.couriertrackingsystem.dto.courier.UpdateCourierRequest;
import com.minelsaygisever.couriertrackingsystem.service.CourierManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/couriers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Courier Management", description = "APIs for managing courier users and tracking their stats")
public class CourierManagementController {

    private final CourierManagementService courierManagementService;

    @GetMapping("/{userId}/total-distance")
    @Operation(
            summary = "Get Real-time Total Distance",
            description = "Returns the total distance traveled by the courier. Includes both persisted data (DB) and live buffered data (Redis) for accuracy."
    )
    public Double getTotalDistance(@PathVariable Long userId) {
        return courierManagementService.getTotalDistance(userId);
    }

    @PostMapping
    @Operation(
            summary = "Create a new Courier",
            description = "Creates a new courier user. Initial total distance is set to 0 automatically."
    )
    public CourierResponse createCourier(@Valid @RequestBody CreateCourierRequest request) {
        return courierManagementService.createCourier(request);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get Courier by User ID", description = "Retrieves detailed information of a specific courier.")
    public CourierResponse getCourier(@PathVariable Long userId) {
        return courierManagementService.getCourierByUserId(userId);
    }

    @GetMapping("/username/{username}")
    @Operation(summary = "Get Courier by Username", description = "Retrieves courier details using their username.")
    public CourierResponse getCourierByUsername(@PathVariable String username) {
        return courierManagementService.getCourierByUsername(username);
    }

    @GetMapping
    @Operation(summary = "List all Couriers", description = "Returns a paginated list of all couriers.")
    public Page<CourierResponse> getCouriers(Pageable pageable) {
        return courierManagementService.getCouriers(pageable);
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update Courier", description = "Updates an existing courier's information.")
    public CourierResponse updateCourier(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateCourierRequest request
    ) {
        return courierManagementService.updateCourier(userId, request);
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete Courier", description = "Deletes a courier user and their associated profile.")
    public void deleteCourier(@PathVariable Long userId) {
        courierManagementService.deleteCourier(userId);
    }
}