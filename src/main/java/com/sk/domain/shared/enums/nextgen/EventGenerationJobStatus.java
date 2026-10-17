package com.sk.domain.shared.enums.nextgen;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * [MCP Event Generate] 이벤트 생성 비동기 작업 상태 (tmd_mcp_job_mi_event_generation_m.status, B.E 소유 어휘).
 * RUNNING 에서 시작해 SUCCEEDED / FAILED / INTERRUPTED 중 하나로 끝난다.
 */
@Getter
@RequiredArgsConstructor
public enum EventGenerationJobStatus {

    RUNNING("생성 중"),
    SUCCEEDED("생성 완료"),
    FAILED("생성 실패"),
    /** 실행하던 B.E 인스턴스가 재기동·종료돼 결과를 받지 못함. 생성 서버는 끝까지 실행해 이벤트를 저장했을 수 있다. */
    INTERRUPTED("결과 수신 중단");

    private final String label;

}
