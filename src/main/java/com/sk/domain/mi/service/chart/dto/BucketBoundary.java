package com.sk.domain.mi.service.chart.dto;

import lombok.Builder;

import java.time.LocalDate;

/**
 * 버킷 경계를 나타내는 내부 레코드 (시작일 포함, 종료일 미포함)
 *
 * @param start
 * @param exclusiveEnd
 */
@Builder
public record BucketBoundary(
    LocalDate start,
    LocalDate exclusiveEnd
) {

    public static BucketBoundary of(LocalDate start, LocalDate exclusiveEnd) {
        return BucketBoundary.builder()
            .start(start)
            .exclusiveEnd(exclusiveEnd)
            .build();
    }

}
