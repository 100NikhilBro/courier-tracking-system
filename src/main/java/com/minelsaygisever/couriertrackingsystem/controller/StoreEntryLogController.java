package com.minelsaygisever.couriertrackingsystem.controller;

import com.minelsaygisever.couriertrackingsystem.dto.store.StoreEntryResponse;
import com.minelsaygisever.couriertrackingsystem.service.StoreEntryLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/store-entries")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Store Entry Logs", description = "View historical records of courier store entries")
public class StoreEntryLogController {

    private final StoreEntryLogService service;

    @GetMapping
    @Operation(summary = "List Store Entries", description = "Returns a paginated list of store entry events.")
    public Page<StoreEntryResponse> getLogs(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return service.getLogEntries(pageable);
    }
}