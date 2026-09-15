package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.shared.entity.master.tmm.MiScenarioGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 수급모델 실행 팝업 진입용 그룹 파라미터 제안 현황.
 */
public record GroupModelParamProposalsRes(

    @Schema(description = "시나리오 그룹 ID", example = "61")
    Long scenarioGroupId,

    @Schema(description = "시나리오 그룹명", example = "중동 리스크 완화와 아프리카 LNG 공급 상쇄")
    String groupName,

    @Schema(description = "그룹 생성일시")
    LocalDateTime groupCreatedAt,

    @Schema(description = "그룹 내 시나리오별 제안 현황 (displayOrder 순)")
    List<GroupModelParamProposalItemRes> scenarios
) {

    public static GroupModelParamProposalsRes of(MiScenarioGroup group, List<GroupModelParamProposalItemRes> scenarios) {
        return new GroupModelParamProposalsRes(
            group.getScenarioGroupId(),
            group.getTitle(),
            group.getCreateDt(),
            scenarios
        );
    }
}
