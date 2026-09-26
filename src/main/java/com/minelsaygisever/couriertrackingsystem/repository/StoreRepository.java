package com.minelsaygisever.couriertrackingsystem.repository;

import com.minelsaygisever.couriertrackingsystem.domain.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {

}
