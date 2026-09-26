package com.minelsaygisever.couriertrackingsystem.repository;

import com.minelsaygisever.couriertrackingsystem.domain.StoreEntryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoreEntryLogRepository extends JpaRepository<StoreEntryLog, Long> {

}
