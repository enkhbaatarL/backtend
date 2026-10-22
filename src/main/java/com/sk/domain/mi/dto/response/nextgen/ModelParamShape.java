package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.shared.enums.nextgen.BasecaseReadMode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 파라미터별 표 모양 (tmm_mi_model_param_m 마스터에서 parameterName으로 조회).
 *
 * FE가 파라미터마다 다른 모양의 표를 렌더링할 수 있도록, 마스터의 컬럼 역할 문자열을
 * {column, role} 목록으로 파싱해 제공한다.
 */
public record ModelParamShape(

    @Schema(description = "수급모델 파라미터명 (parameters[].parameterName과 매칭되는 자연키)", example = "PowerMix")
    String parameterName,

    @Schema(description = "모델 파일 구분", example = "Demand")
    String modelFile,

    @Schema(description = "모델 시트명", example = "PowerMixAdjustment")
    String modelSheet,

    @Schema(description = "모델 시트 내 테이블명", example = "table1")
    String modelTable,

    @Schema(description = "테이블 화면 표시명 (카탈로그 매핑 불가 시 modelTable 그대로)", example = "Annual Power Mix Adjustment")
    String displayTableName,

    @Schema(
        description = "표 렌더링 방식. FIXED_COLUMNS=행 단위로 값이 배치된 형태, "
            + "MATRIX_TIME_COLUMNS=월별 값이 열 방향으로 배치된 형태",
        implementation = BasecaseReadMode.class,
        example = "FIXED_COLUMNS"
    )
    BasecaseReadMode readMode,

    @Schema(description = "컬럼 역할 목록 (basecase_column_roles 파싱)")
    List<ModelParamColumn> columns,

    @Schema(description = "의미 축 설명 (원문 문자열)", example = "slot=B:Year(YYYY, 2026~2030), C:Country(...), D:Energy Source(...); value=F:% Power Mix, 소수형 발전 비중")
    String semanticAxes
) {
}
