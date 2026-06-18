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

    HH(IndexName.HH, 1020031L, "CME_NG_FUTURES"),
    JKM(IndexName.JKM, 1020432L, "JKM_Futures"),
    TTF(IndexName.TTF, 1020809L, "CME_TTF_USD");

    private final IndexName indexName;
    private final long indexId;
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
