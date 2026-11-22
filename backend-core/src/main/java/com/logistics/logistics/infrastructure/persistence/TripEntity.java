package com.logistics.logistics.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.logistics.logistics.domain.EtaSource;
import com.logistics.logistics.domain.TripStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Mapeamento JPA da tabela trips. O schema é controlado pelo Flyway (db/migration). */
@Entity
@Table(name = "trips")
@Getter @Setter @NoArgsConstructor
public class TripEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private Double originLat;
    @Column(nullable = false)
    private Double originLng;
    @Column(nullable = false)
    private Double destLat;
    @Column(nullable = false)
    private Double destLng;

    @Column(length = 200)
    private String originLabel;
    @Column(length = 200)
    private String destinationLabel;

    @Column(nullable = false)
    private Double payloadWeight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TripStatus status;

    @Column(nullable = false)
    private OffsetDateTime departureTime;
    private OffsetDateTime predictedArrival;
    private OffsetDateTime actualArrival;

    @Column(length = 100)
    private String etaModelVersion;
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EtaSource etaSource;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
