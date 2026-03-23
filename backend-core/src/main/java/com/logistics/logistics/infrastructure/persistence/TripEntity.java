package com.logistics.logistics.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.UUID;


import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "trips")
@Getter @Setter @NoArgsConstructor
public class TripEntity {
    public static final String TripStatus = null;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Double originLat;
    private Double originLng;
    private Double destLat;
    private Double destLng;
    private Double payloadWeight;

    @Enumerated(EnumType.STRING)
    private TripStatus status;

    public enum TripStatus {
        PENDING,
        IN_TRANSIT,
        COMPLETED,
        DELAYED
    }

    private LocalDateTime predictedArrival;
    private LocalDateTime createdAt = LocalDateTime.now();


}
