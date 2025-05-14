package com.sk.domain.mi.dto.response.tactics;

import com.sk.domain.shared.dto.AuditInfo;
import com.sk.domain.shared.entity.master.tcm.PolicyTactics;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
public record PolicyTacticsSimpleRes(

    @Schema(description = "정책약관 번호", example = "1234")
    Integer poltacNo,

    @Schema(description = "정책약관 버전명", example = "v1")
    String poltacVerNm,

    AuditInfo auditInfo

) {

    public static PolicyTacticsSimpleRes of(PolicyTactics entity) {
        return PolicyTacticsSimpleRes.builder()
            .poltacNo(entity.getPoltacNo())
            .poltacVerNm(entity.getPoltacVersionNm())
            .auditInfo(AuditInfo.ofSimple(
                entity.getCreateUserId(),
                entity.getCreateDt(),
                entity.getModifyUserId(),
                entity.getModifyDt()
            ))
            .build();
    }

    public static List<PolicyTacticsSimpleRes> ofList(List<PolicyTactics> entities) {
        return entities.stream()
            .map(PolicyTacticsSimpleRes::of)
            .toList();
    }

}
