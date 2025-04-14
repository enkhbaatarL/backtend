package com.sk.domain.mi.dto.response.privatePolicy;

import com.sk.domain.shared.entity.master.tcm.PrivatePolicy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

// @formatter:off
@Builder
public record MiPrivatePolicyVersionRes(
        @Schema(description = "정책약관번호", example = "1") 
        Integer poltacNo,

        @Schema(description = "정책약관버전명", example = "sample.jpg") 
        String poltacVerNm) {

        public static MiPrivatePolicyVersionRes of(PrivatePolicy privatePolicy) {
                return MiPrivatePolicyVersionRes.builder()
                .poltacNo(privatePolicy.getPoltacNo())
                .poltacVerNm(privatePolicy.getPoltacVersionNm())
                .build();
        }
}
// @formatter:on