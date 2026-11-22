package com.logistics.logistics.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.logistics.logistics.domain.TripStatus;

public interface TripJpaRepository extends JpaRepository<TripEntity, UUID> {

  Page<TripEntity> findByStatus(TripStatus status, Pageable pageable);
}
