package com.sk.domain.shared.enums.nextgen;

import java.util.Set;

/**
 * 수급모델 실행 입력 방식 (Scenario -> ScenarioGroup -> eventTypeL2Id 관계로 Backend가 판정).
 *
 * <ul>
 *   <li>{@code PARAMETER_PROPOSAL}: Agent 파라미터 제안 대상. 제안 생성/수정 후 JSON execution 사용.</li>
 *   <li>{@code FILE_UPLOAD}: 제안 미대상. 사용자가 Excel 3개를 직접 업로드해 기존 multipart execution 사용.</li>
 * </ul>
 *
 * <p>미대상(FILE_UPLOAD) 집합은 canonical design 3.4절/feature-spec 5.0의 eligibility 계약과 동일하며,
 * 이 enum이 유일한 소스다. (2026-07-07: {@code eventTypeL2Id=3} 추가 → {@code IN (3,12,13,14,15,16,20)})
 * 그룹 그래프 API의 {@code parameterProposalTarget}, 향후 model-execution-options API가 함께 참조한다.
 */
public enum ModelInputMode {

    PARAMETER_PROPOSAL,
    FILE_UPLOAD;

    /** 파라미터 제안 미대상(FILE_UPLOAD) eventTypeL2Id 집합 — 단일 소스. */
    private static final Set<Long> FILE_UPLOAD_EVENT_TYPE_L2_IDS = Set.of(3L, 12L, 13L, 14L, 15L, 16L, 20L);

    /**
     * eventTypeL2Id로 입력 방식을 판정한다.
     * 제외 목록에 있으면 {@code FILE_UPLOAD}, 그 외(및 null)는 {@code PARAMETER_PROPOSAL}.
     */
    public static ModelInputMode from(Long eventTypeL2Id) {
        return eventTypeL2Id != null && FILE_UPLOAD_EVENT_TYPE_L2_IDS.contains(eventTypeL2Id)
            ? FILE_UPLOAD
            : PARAMETER_PROPOSAL;
    }

    /** 파라미터 제안 대상 여부. */
    public boolean isParameterProposal() {
        return this == PARAMETER_PROPOSAL;
    }

}
