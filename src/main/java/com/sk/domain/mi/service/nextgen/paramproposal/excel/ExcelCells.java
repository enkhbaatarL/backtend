package com.sk.domain.mi.service.nextgen.paramproposal.excel;

import com.sk.domain.shared.enums.nextgen.BasecaseStopRule;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;
import org.apache.poi.ss.util.CellReference;

/**
 * basecase 병합 전용 셀 read/비교 헬퍼.
 *
 * 값 정규화 규칙: 숫자 셀은 Double, 문자 셀은 trim String, 빈 문자열/BLANK/ERROR(#REF! 등)는 null.
 * 셀 접근은 MergeSheet 추상화를 통한다 — 실제 판독·기입은 XlsmStreamMerger가 담당한다.
 */
final class ExcelCells {

    /** 숫자 비교 허용 오차 — payload 값은 같은 셀에서 추출된 double이라 사실상 동일하나 직렬화 잡음만 흡수한다. */
    private static final double EPSILON = 1e-9;

    private ExcelCells() {
    }

    /**
     * 행 스캔 결과.
     * matchedRowIndexes는 조건 일치 행, appendRowIndex는 마지막 데이터 행 바로 다음(신규 행 기입 위치)이다.
     */
    record RowScan(List<Integer> matchedRowIndexes, int appendRowIndex) {
    }

    /**
     * startRowIndex부터 key 컬럼 기준으로 데이터 영역을 스캔한다. 종료 시점은 마스터의 basecase_stop_rule을 따른다.
     * 데이터 행마다 rowMatches 판정으로 일치 행을 수집한다.
     */
    static RowScan scanRows(MergeSheet sheet, BasecaseStopRule stopRule, int startRowIndex, List<Integer> keyColumnIndexes, IntPredicate rowMatches) {
        return switch (stopRule) {
            case STOP_WHEN_ALL_KEY_COLUMNS_EMPTY -> scanUntilAllKeyColumnsEmpty(sheet, startRowIndex, keyColumnIndexes, rowMatches);
        };
    }

    /**
     * key 컬럼이 모두 빈 첫 행에서 종료한다 (Agent의 Basecase 읽기와 동일 범위).
     * 한 시트에 같은 key 구조의 표가 빈 행을 사이에 두고 이어지는 경우(예: ShippingCostInput)가 있어, 빈 행을 넘어 다음 표까지 읽으면 안 된다.
     */
    private static RowScan scanUntilAllKeyColumnsEmpty(MergeSheet sheet, int startRowIndex, List<Integer> keyColumnIndexes, IntPredicate rowMatches) {
        List<Integer> matched = new ArrayList<>();
        int rowIndex = startRowIndex;
        for (; !isRowBlank(sheet, rowIndex, keyColumnIndexes); rowIndex++) {
            if (rowMatches.test(rowIndex)) {
                matched.add(rowIndex);
            }
        }
        return new RowScan(matched, rowIndex);
    }

    /** 컬럼 문자를 0-based 인덱스로 변환한다. */
    static int columnIndex(String columnLetter) {
        return CellReference.convertColStringToIndex(columnLetter.trim());
    }

    /** 셀 값을 정규화해 읽는다. 값이 없거나 판독 불가(ERROR 등)면 null. */
    static Object readValue(MergeSheet sheet, int rowIndex, int columnIndex) {
        return sheet.readValue(rowIndex, columnIndex);
    }

    /** 지정 컬럼들이 모두 빈 값인 행인지 판정한다. */
    static boolean isRowBlank(MergeSheet sheet, int rowIndex, List<Integer> columnIndexes) {
        return columnIndexes.stream().allMatch(col -> readValue(sheet, rowIndex, col) == null);
    }

    /**
     * payload 값과 셀 값의 일치 판정.
     * 숫자끼리는 오차 비교, 그 외는 trim 문자열 비교. 한쪽만 숫자면 문자열 표현으로 정규화해 비교한다.
     */
    static boolean matches(Object expected, Object actual) {
        if (expected == null || actual == null) {
            return false;
        }
        Double expectedNumber = asNumber(expected);
        Double actualNumber = asNumber(actual);
        if (expectedNumber != null && actualNumber != null) {
            return Math.abs(expectedNumber - actualNumber) < EPSILON;
        }
        return normalize(expected).equals(normalize(actual));
    }

    /** 숫자로 해석 가능하면 Double, 아니면 null. */
    static Double asNumber(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static String normalize(Object value) {
        if (value instanceof Number number) {
            double d = number.doubleValue();
            return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
        }
        return String.valueOf(value).trim();
    }

    /** 엑셀 셀 주소 표기 (예: AY11) — 로그/에러는 항상 엑셀 1-based 표기로 말한다. */
    static String cellAddress(int rowIndex, int columnIndex) {
        return CellReference.convertNumToColString(columnIndex) + (rowIndex + 1);
    }

    /** 로그 표기용 값 문자열 — 정수성 숫자는 소수점 없이, null은 (없음). */
    static String display(Object value) {
        if (value == null) {
            return "(없음)";
        }
        if (value instanceof Number number) {
            double d = number.doubleValue();
            return d == Math.rint(d) && !Double.isInfinite(d) ? String.valueOf((long) d) : String.valueOf(d);
        }
        return String.valueOf(value);
    }

}
