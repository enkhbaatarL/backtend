package com.sk.domain.mi.dto.response.marketData;

import java.time.LocalDate;

import lombok.Builder;

public record CalculatePeriodChangeRange(LocalDate currentStart, LocalDate currentEnd,
        LocalDate previousStart, LocalDate previousEnd) {

    @Builder
    public CalculatePeriodChangeRange(LocalDate currentStart, LocalDate currentEnd,
            LocalDate previousStart, LocalDate previousEnd) {
        this.currentStart = currentStart;
        this.currentEnd = currentEnd;
        this.previousStart = previousStart;
        this.previousEnd = previousEnd;
    }
}