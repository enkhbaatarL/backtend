package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 시나리오 차원값 (표/개별 모드의 한 칼럼 셀, 시나리오 상세의 주요 가정 카드). 그룹별 동적.
 * value는 항상 표시 label이다 — 검토/Signpost 요청의 셀 값으로 그대로 재전송하면 Agent 계약(cells=표시 label)이 충족된다.
 * valueId/rationale은 v4(definitions 기반) 그룹에서만 채워지며 v1은 null이다.
 */
public record ScenarioDimensionRes(

    @Schema(description = "차원 키 (dimensionColumns의 key와 매칭)", example = "d1")
    String key,

    @Schema(description = "차원 명", example = "유럽 저장률 개선 지속성 판단")
    String label,

    @Schema(description = "차원 값 (표시 label — 검토/Signpost 요청에 이 값을 그대로 재전송)", example = "순주입 흐름 약화 전환 확인")
    String value,

    @Schema(description = "차원 값 표시 라벨 (value와 동일 — 하위호환 유지 필드)", example = "순주입 흐름 약화 전환 확인")
    String valueLabel,

    @Schema(description = "분기값 ID (dimensionValueOptions의 valueId와 매칭 — 드롭다운 선택 상태 표시용, 요청에는 미전송. v1은 null)", example = "d1_v4", nullable = true)
    String valueId,

    @Schema(description = "값 선정 근거 (셀 (i) 아이콘, v4 전용. v1은 null)", nullable = true)
    String rationale

) {}
