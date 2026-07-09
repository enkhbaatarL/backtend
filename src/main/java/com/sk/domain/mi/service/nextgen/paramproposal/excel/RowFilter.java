package com.sk.domain.mi.service.nextgen.paramproposal.excel;

import org.apache.poi.ss.usermodel.Sheet;

/**
 * 마스터의 basecase_row_filter 파서
 *
 * 예: E=CDD — WeatherAdjustment처럼 한 표에 성격이 다른 행(HDD/CDD)이 섞여 있어
 * slot만으로 행이 유일해지지 않는 표에서, 파라미터가 항상 달고 다니는 고정 행 조건이다.
 * 해당 컬럼 값이 일치하는 행만 대상 후보로 보며, 신규 행 추가 시에는 필터 값도 함께 기입해야 한다.
 */
record RowFilter(int columnIndex, String expected) {

    /** 필터 스펙(컬럼문자=값)을 파싱한다. 스펙이 없으면 null(필터 없음). */
    static RowFilter parse(String context, String spec) {
        if (spec == null || spec.isBlank()) {
            return null;
        }
        int separator = spec.indexOf('=');
        if (separator < 1) {
            throw ValueRows.error(context, "마스터의 row filter 형식 오류 (컬럼문자=값 형식이어야 함): %s".formatted(spec));
        }
        String columnLetter = spec.substring(0, separator).trim();
        String expected = spec.substring(separator + 1).trim();
        if (!columnLetter.matches("[A-Za-z]{1,3}") || expected.isEmpty()) {
            throw ValueRows.error(context, "마스터의 row filter 형식 오류 (컬럼문자=값 형식이어야 함): %s".formatted(spec));
        }
        return new RowFilter(ExcelCells.columnIndex(columnLetter), expected);
    }

    /** 해당 행이 필터 조건을 만족하는지 판정한다. */
    boolean matches(Sheet sheet, int rowIndex) {
        return ExcelCells.matches(expected, ExcelCells.readValue(sheet, rowIndex, columnIndex));
    }

}
