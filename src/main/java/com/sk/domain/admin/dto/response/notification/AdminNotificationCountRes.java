package com.sk.domain.admin.dto.response.notification;

import java.time.LocalDateTime;

import com.querydsl.core.annotations.QueryProjection;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record AdminNotificationCountRes(

        @Schema(description = "이메일 총 발송 건수", example = "10") Integer totalEmailSendCnt,

        @Schema(description = "이메일 총 성공 건수", example = "9") Integer totalEmailSuccessCnt,

        @Schema(description = "이메일 총 실패 건수", example = "1") Integer totalEmailFailCnt,

        @Schema(description = "마지막 업데이트 일시", example = "2025-01-01 00:00:00") LocalDateTime lastUpdatedDt) {

    @Builder
    @QueryProjection
    public AdminNotificationCountRes(Integer totalEmailSendCnt, Integer totalEmailSuccessCnt, Integer totalEmailFailCnt,
            LocalDateTime lastUpdatedDt) {
        this.totalEmailSendCnt = totalEmailSendCnt;
        this.totalEmailSuccessCnt = totalEmailSuccessCnt;
        this.totalEmailFailCnt = totalEmailFailCnt;
        this.lastUpdatedDt = lastUpdatedDt;
    }
}
