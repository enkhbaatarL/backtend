package com.sk.domain.admin.dto.response.privatePolicy;

import com.sk.domain.shared.entity.master.tcm.PrivatePolicy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

// @formatter:off
@Builder
public record PrivatePolicyVersionRes(
        @Schema(description = "정책약관번호", example = "1") 
        Integer poltacNo,

        @Schema(description = "정책약관버전명", example = "sample.jpg") 
        String poltacVerNm) {

        public static PrivatePolicyVersionRes of(PrivatePolicy privatePolicy) {
                return PrivatePolicyVersionRes.builder()
                .poltacNo(privatePolicy.getPoltacNo())
                .poltacVerNm(privatePolicy.getPoltacVersionNm())
                .build();
        }
}
// @formatter:on