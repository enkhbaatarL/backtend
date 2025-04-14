package com.sk.domain.mi.dto.response.privatePolicy;

import java.time.format.DateTimeFormatter;

import com.sk.domain.shared.entity.master.tcm.PrivatePolicy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

// @formatter:off
@Builder
public record MiPrivatePolicyRes(
        @Schema(description = "정책약관번호", example = "1") 
        Integer poltacNo,

        @Schema(description = "정책약관버전명", example = "2025-03-25") 
        String poltacVerNm,

        @Schema(description = "정책약관시행일시", example = "2025-02-12") 
        String poltacEnforcementDt,

        @Schema(description = "정책약관개정일시", example = "2025-02-12") 
        String poltacRevisionDt,

        @Schema(description = "정책약관내용", example = "") 
        String poltacCntt) {

    public static MiPrivatePolicyRes of(PrivatePolicy privatePolicy) {
        return MiPrivatePolicyRes.builder()
                .poltacNo(privatePolicy.getPoltacNo())
                .poltacVerNm(privatePolicy.getPoltacVersionNm())
                .poltacEnforcementDt(privatePolicy.getPoltacEnforcementDt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .poltacRevisionDt(privatePolicy.getModifyDt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .poltacCntt(privatePolicy.getPoltacCntt())
                .build();
    }
}
// @formatter:on