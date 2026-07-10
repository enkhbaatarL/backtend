package com.sk.domain.mi.dto.response.nextgen;

/**
 * 표 컬럼 종류.
 *
 * - SLOT: 행을 식별하는 축 컬럼 (payload modelTarget.slotAxesFieldNames에 해당)
 * - VALUE: basecaseValue/proposedValue가 들어가는 값 컬럼
 */
public enum ModelParamColumnKind {
    SLOT,
    VALUE
}
