package com.sk.domain.mi.dto.response.nextgen;

/**
 * 표 컬럼 정렬. 현재는 데이터 타입 기준으로 숫자=RIGHT, 문자=LEFT만 사용한다.
 * (CENTER는 향후 규칙 확장 여지를 위해 정의만 둔다)
 */
public enum ModelParamAlign {
    LEFT,
    CENTER,
    RIGHT
}
