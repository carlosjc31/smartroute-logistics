package com.logistics.logistics.infrastructure.controller.dto;

import com.logistics.logistics.domain.TripStatus;

import jakarta.validation.constraints.NotNull;

public record TripStatusUpdateDTO(@NotNull TripStatus status) {}
