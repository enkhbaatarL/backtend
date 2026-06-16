package com.sk.domain.mi.dto.request.nextgen.design;

import com.sk.domain.mi.dto.response.nextgen.design.DesignEditTable;
import jakarta.validation.constraints.NotNull;

/**
 * 단계1 변경사항 검토 요청.
 * <p>
 * 편집 후 분기항목/시나리오 표를 전달한다 (미채번 행=신규, 빠진 행=삭제). 그룹 메타는 사용자가 수정 못하는 영역이라 받지 않는다.
 * B.E가 Agent 계약(cells map 등) 형태로 변환해 검토 요청을 프록시한다.
 */
public record ScenarioValidationReq(
    @NotNull DesignEditTable table
) {}
