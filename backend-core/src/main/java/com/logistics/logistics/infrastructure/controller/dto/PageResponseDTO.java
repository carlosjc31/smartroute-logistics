package com.logistics.logistics.infrastructure.controller.dto;

import java.util.List;
import java.util.function.Function;

import com.logistics.logistics.application.PageResult;

public record PageResponseDTO<T>(List<T> items, int page, int size, long totalItems, int totalPages) {
  public static <D, T> PageResponseDTO<T> from(PageResult<D> page, Function<D, T> mapper) {
    PageResult<T> mapped = page.map(mapper);
    return new PageResponseDTO<>(mapped.items(), mapped.page(), mapped.size(), mapped.totalItems(), mapped.totalPages());
  }
}
