package com.sk.domain.mi.dto.response.tactics;

import com.sk.common.utils.DateUtil;
import com.sk.domain.shared.dto.AuditInfo;
import com.sk.domain.shared.entity.master.tcm.PolicyTactics;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record PolicyTacticsRes(

    @Schema(description = "정책약관 번호", example = "1004")
    Integer poltacNo,

    @Schema(description = "정책약관 버전명", example = "버전1.0")
    String poltacVerNm,

    @Schema(description = "정책약관 시행일시", example = "20250212")
    String poltacEnforcementDt,

    @Schema(description = "정책약관 내용", example = "")
    String poltacCntt,

    AuditInfo auditInfo

) {

    public static PolicyTacticsRes of(PolicyTactics entity) {
        return PolicyTacticsRes.builder()
            .poltacNo(entity.getPoltacNo())
            .poltacVerNm(entity.getPoltacVersionNm())
            .poltacEnforcementDt(DateUtil.toYmd(entity.getPoltacEnforcementDt()))
            .poltacCntt(entity.getPoltacCntt())
            .auditInfo(AuditInfo.ofSimple(
                entity.getCreateUserId(),
                entity.getCreateDt(),
                entity.getModifyUserId(),
                entity.getModifyDt()
            ))
            .build();
    }

}
