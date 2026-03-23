package com.logistics.logistics.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.logistics.logistics.domain.Trip.TripStatus;

@Repository
public interface TripRepository extends JpaRepository<TripEntity, UUID> {

  List<TripEntity> findByStatus(TripStatus status);
}
