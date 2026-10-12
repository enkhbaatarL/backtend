package com.sk.domain.mi.dto.request.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EventKeywordSettingReq() {

    public record Insert(

        @NotBlank(message = "키워드를 입력하세요.")
        @Schema(description = "키워드 (정규화 후 2~30자)", example = "호르무즈")
        String keywordName

    ) {
    }

    public record UpdateNotificationUse(

        @NotNull
        @Schema(description = "키워드 알림 사용 여부", example = "true")
        Boolean isNotificationUse

    ) {
    }

}
