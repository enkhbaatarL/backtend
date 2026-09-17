package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 시나리오 파라미터 비교 그리드 응답.
 * 완료 시나리오들의 제안 파라미터를 slot 축 기준으로 행 병합한 결과.
 * 단건 조회 대비 순수 추가분은 scenarios(열 정의), row의 cells(시나리오별 값), scenarioRationales뿐이고
 * 나머지(파라미터 row 형태, parameterShapes, modelFiles)는 단건 조회와 같은 구조를 유지한다.
 */
public record GroupModelParamCompareRes(

    @Schema(description = "시나리오 그룹 ID", example = "61")
    Long scenarioGroupId,

    @Schema(description = "비교 대상(완료) 시나리오 열 목록. displayOrder 순이며 각 row의 cells와 같은 순서")
    List<GroupModelParamCompareScenarioRes> scenarios,

    @Schema(description = "파라미터별 병합 그리드")
    List<GroupModelParamCompareParameterRes> parameters,

    @Schema(description = "parameters의 parameterName별 표 모양 (단건 조회와 동일 구조·위치)")
    List<ModelParamShape> parameterShapes,

    @Schema(description = "파라미터들의 modelFile > modelSheet > tableName 계층 (시나리오 전체 union)")
    List<ModelFileNode> modelFiles
) {
}
