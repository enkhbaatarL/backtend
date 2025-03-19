package com.sk.domain.admin.dto.response.company;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

public record LatestUpdatedRes(

    @Schema(description = "수정자 ID", example = "sk01234")
    String modifyUserId,

    @Schema(description = "수정자 명", example = "아이유")
    String modifyUserNm,

    @Schema(description = "수정 일시", example = "2025-02-12 15:00:00")
    LocalDateTime modifyDt

) {

    @Builder
    public LatestUpdatedRes(String modifyUserId, String modifyUserNm, LocalDateTime modifyDt) {
        this.modifyUserId = modifyUserId;
        this.modifyUserNm = modifyUserNm;
        this.modifyDt = modifyDt;
    }

    public static LatestUpdatedRes of(String modifyUserId, String modifyUserNm, LocalDateTime modifyDt) {
        return LatestUpdatedRes.builder()
            .modifyUserId(modifyUserId)
            .modifyUserNm(modifyUserNm)
            .modifyDt(modifyDt)
            .build();
    }

}
