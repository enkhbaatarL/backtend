package com.sk.domain.mi.service.chart.dto;

import java.time.LocalDate;

// 버킷 경계를 나타내는 내부 레코드 (시작일 포함, 종료일 미포함)
public record BucketBoundary(
    LocalDate start,
    LocalDate exclusiveEnd
) {
}
