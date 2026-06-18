package com.sk.domain.shared.enums.nextgen;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * CME 벤치마크 인덱스 매핑 {@code tif_skgas_odin.index_id}).
 * 출처: 수급모델 > 시나리오 전망 벤치마크 차트 기획서.
 * 현재는 하드코딩. 추후 벤치마크 관리 테이블(대상정보·DB·테이블·index_id)로 이관 예정
 */
@Getter
@RequiredArgsConstructor
public enum CmeBenchmarkIndex {

    HH(IndexName.HH, "1020031", "CME_NG_FUTURES"),
    JKM(IndexName.JKM, "1020432", "JKM_Futures"),
    TTF(IndexName.TTF, "1020809", "CME_TTF_USD");

    private final IndexName indexName;
    /** tif_skgas_odin.index_id (VARCHAR). 실DB가 문자열로 적재 — 숫자 비교 시 암시변환·인덱스 미사용 회피. */
    private final String indexId;
    private final String cmeIndexName;

    public static CmeBenchmarkIndex from(IndexName indexName) {
        for (CmeBenchmarkIndex value : values()) {
            if (value.indexName == indexName) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unsupported CME benchmark index: " + indexName);
    }
}
