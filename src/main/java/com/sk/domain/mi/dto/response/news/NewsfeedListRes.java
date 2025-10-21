package com.sk.domain.mi.dto.response.news;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 뉴스피드 목록 응답 DTO
 */
public record NewsfeedListRes(
        Long nwsSigId,
        Long dataRegistrationSn,
        LocalDateTime firstCreated,
        String nwsMrktCtgrCd,
        BigDecimal sig
) {
}
