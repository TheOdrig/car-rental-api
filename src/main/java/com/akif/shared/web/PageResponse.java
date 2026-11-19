package com.akif.shared.web;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> of(Page<T> source) {
        return new PageResponse<>(
                source.getContent(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages());
    }

    public <R> PageResponse<R> map(java.util.function.Function<? super T, ? extends R> mapper) {
        return new PageResponse<>(content.stream().<R>map(mapper).toList(), page, size, totalElements, totalPages);
    }
}
