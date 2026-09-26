package com.minelsaygisever.couriertrackingsystem.service;

import com.minelsaygisever.couriertrackingsystem.domain.StoreEntryLog;
import com.minelsaygisever.couriertrackingsystem.dto.store.StoreEntryResponse;
import com.minelsaygisever.couriertrackingsystem.repository.StoreEntryLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoreEntryLogService {

    private final StoreEntryLogRepository repository;

    @Transactional(readOnly = true)
    public Page<StoreEntryResponse> getLogEntries(Pageable pageable) {
        return repository.findAll(pageable)
                .map(this::mapToResponse);
    }

    private StoreEntryResponse mapToResponse(StoreEntryLog log) {
        return StoreEntryResponse.builder()
                .id(log.getId())
                .courierUsername(log.getCourier().getUser().getUsername())
                .courierFullName(log.getCourier().getFullName())
                .storeName(log.getStore().getName())
                .actualLatitude(log.getActualLatitude())
                .actualLongitude(log.getActualLongitude())
                .entryTime(log.getCreatedAt())
                .build();
    }
}