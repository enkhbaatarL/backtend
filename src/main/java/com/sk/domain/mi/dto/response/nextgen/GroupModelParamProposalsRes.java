package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.mi.service.nextgen.paramproposal.GroupMiBaseResolver.GroupMiBaseStatus;
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
    List<GroupModelParamProposalItemRes> scenarios,

    @Schema(description = "그룹에 적용된 MI Base (생성 결과가 없거나 초기화 후면 null)")
    MiBaseRefRes appliedMiBase,

    @Schema(description = "현재 지정된 MI Base ([현재 MI Base 보기] 이동용, 지정된 MI Base가 없으면 null)")
    MiBaseRefRes currentMiBase,

    @Schema(description = "MI Base 변경 여부 (그룹에 적용된 MI Base와 현재 MI Base가 다르면 true)", example = "false")
    boolean miBaseChanged
) {

    public static GroupModelParamProposalsRes of(
        MiScenarioGroup group,
        List<GroupModelParamProposalItemRes> scenarios,
        GroupMiBaseStatus miBaseStatus
    ) {
        return new GroupModelParamProposalsRes(
            group.getScenarioGroupId(),
            group.getTitle(),
            group.getCreateDt(),
            scenarios,
            miBaseStatus.applied(),
            miBaseStatus.current(),
            miBaseStatus.changed()
        );
    }
}
