package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.shared.entity.master.tmd.EventKeywordSetting;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;

public record EventKeywordSettingRes(

    @Schema(description = "키워드 설정 ID", example = "1")
    Long keywordSettingId,

    @Schema(description = "키워드", example = "호르무즈")
    String keywordName,

    @Schema(description = "키워드 알림 사용 여부", example = "true")
    Boolean isNotificationUse,

    @Schema(description = "등록 일시", example = "2026-10-08 10:00:00")
    LocalDateTime createdAt

) {

    @Builder
    public EventKeywordSettingRes {
    }

    public static EventKeywordSettingRes of(EventKeywordSetting setting) {
        return EventKeywordSettingRes.builder()
                .keywordSettingId(setting.getKeywordSettingId())
                .keywordName(setting.getKeywordName())
                .isNotificationUse(setting.getIsNotificationUse())
                .createdAt(setting.getCreateDt())
                .build();
    }

}
