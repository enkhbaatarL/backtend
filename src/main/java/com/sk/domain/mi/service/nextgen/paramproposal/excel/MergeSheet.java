package com.sk.domain.mi.service.nextgen.paramproposal.excel;

/**
 * 병합 기입기가 보는 시트 추상화 (패키지 외부 비공개).
 *
 * POI Sheet 의존을 끊기 위한 경계 — 기입 로직(Fixed/MatrixWriter)은 이 인터페이스만 보고,
 * 실제 구현은 시트 XML을 스트리밍 파싱한 값 맵(XlsmStreamMerger.XmlSheet)이다.
 *
 * 값 정규화 규칙은 기존 POI 구현과 동일: 숫자 셀은 Double, 문자 셀은 trim String(빈 문자열은 null),
 * BLANK/ERROR는 null, 수식 셀은 캐시된 결과값.
 */
interface MergeSheet {

    /** 셀 값을 정규화해 읽는다. 값이 없거나 판독 불가면 null. */
    Object readValue(int rowIndex, int columnIndex);

    /**
     * 셀에 값을 기입한다. 숫자성 값은 숫자로, 그 외는 문자열로 쓴다.
     * 기입 전에 있던 값을 반환한다 (로그의 이전값 -> 새값 표기용, 없었으면 null).
     * 기입 결과는 같은 병합 내 이후 read/scan에 즉시 보인다 — 앞선 값 행의 신규 행 추가가 뒤 행 스캔에 반영되는 기존 동작과 동일.
     */
    Object writeValue(int rowIndex, int columnIndex, Object value);

}
