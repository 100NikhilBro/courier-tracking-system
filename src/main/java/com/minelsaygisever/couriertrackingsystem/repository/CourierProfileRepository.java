package com.minelsaygisever.couriertrackingsystem.repository;

import com.minelsaygisever.couriertrackingsystem.domain.CourierProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface CourierProfileRepository extends JpaRepository<CourierProfile, Long> {
    Optional<CourierProfile> findByUserId(Long userId);
    Optional<CourierProfile> findByUser_Username(String username);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE CourierProfile c SET c.totalDistanceInMeters = c.totalDistanceInMeters + :distance WHERE c.user.id = :userId")
    void addDistanceToCourier(@Param("userId") Long userId, @Param("distance") Double distance);
}
