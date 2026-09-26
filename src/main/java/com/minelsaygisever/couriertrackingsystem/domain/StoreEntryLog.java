package com.minelsaygisever.couriertrackingsystem.domain;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "store_entry_logs")
public class StoreEntryLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_profile_id", nullable = false)
    private CourierProfile courier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "actual_lat", nullable = false)
    private double actualLatitude;

    @Column(name = "actual_lon", nullable = false)
    private double actualLongitude;
}