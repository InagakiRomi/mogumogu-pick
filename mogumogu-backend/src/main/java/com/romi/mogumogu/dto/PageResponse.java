package com.romi.mogumogu.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record PageResponse<T>(
        List<T> data,
        Integer page,
        Integer limit,
        Long total) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements());
    }
}
