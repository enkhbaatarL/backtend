package com.sk.domain.mi.dto.response.nextgen;

/**
 * 표 컬럼 종류.
 *
 * - SLOT: 행을 식별하는 (왼쪽 고정) 축 컬럼
 * - TIME_AXIS: MATRIX_TIME_COLUMNS에서 가로 시간 컬럼 헤더를 구성하는 축 (예: Year/Month/Period).
 *   실제 시간 컬럼은 values[]의 distinct 튜플로 FE가 생성한다.
 * - VALUE: basecaseValue/proposedValue가 들어가는 값 컬럼
 */
public enum ModelParamColumnKind {
    SLOT,
    TIME_AXIS,
    VALUE
}
