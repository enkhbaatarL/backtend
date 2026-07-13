package com.sk.domain.mi.service.nextgen.paramproposal.excel;

/**
 * Writer 기입 결과 집계 — 병합 최종 DONE 요약 로그의 재료 (패키지 외부 비공개).
 */
record WriteSummary(int writeCount, int appendCount) {

    static WriteSummary sum(WriteSummary a, WriteSummary b) {
        return new WriteSummary(a.writeCount + b.writeCount, a.appendCount + b.appendCount);
    }

}
