package com.minelsaygisever.couriertrackingsystem.repository;

import com.minelsaygisever.couriertrackingsystem.domain.AdminProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminProfileRepository extends JpaRepository<AdminProfile, Long> {
    Optional<AdminProfile> findByUser_Username(String username);
    Optional<AdminProfile> findByUserId(Long userId);

}
