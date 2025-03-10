package com.sk.domain.admin.dto.response;

import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.entity.master.tif.CompanyInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

public record CompanyInfoRes(

    @Schema(description = "회사명", example = "SK이노베이션")
    String companyNm,

    @Schema(description = "회사 영문명", example = "skinnovation")
    String companyEngNm,

    @Schema(description = "등록자", example = "sk01234")
    String createUserId,

    @Schema(description = "등록일시", example = "2025-02-12 15:00:00")
    LocalDateTime createDt,

    @Schema(description = "수정자", example = "sk01234")
    String modifyUserId,

    @Schema(description = "수정일시", example = "2025-02-12 15:00:00")
    LocalDateTime modifyDt

) {

    @Builder
    @QueryProjection
    public CompanyInfoRes(String companyNm, String companyEngNm, String createUserId, LocalDateTime createDt, String modifyUserId, LocalDateTime modifyDt) {
        this.companyNm = companyNm;
        this.companyEngNm = companyEngNm;
        this.createUserId = createUserId;
        this.createDt = createDt;
        this.modifyUserId = modifyUserId;
        this.modifyDt = modifyDt;
    }

    public static CompanyInfoRes of(CompanyInfo savedEntity) {
        return CompanyInfoRes.builder()
            .companyNm(savedEntity.getCompanyNm())
            .companyEngNm(savedEntity.getCompanyEngNm())
            .createUserId(savedEntity.getCreateUserId())
            .createDt(savedEntity.getCreateDt())
            .modifyUserId(savedEntity.getModifyUserId())
            .modifyDt(savedEntity.getModifyDt())
            .build();
    }

}
