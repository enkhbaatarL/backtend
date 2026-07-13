package com.sk.domain.mi.service.nextgen.paramproposal.excel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellReference;

/**
 * basecase 병합 전용 셀 read/write/비교 헬퍼.
 *
 *
 * 값 정규화 규칙: 숫자 셀은 Double, 문자 셀은 trim String, 빈 문자열/BLANK/ERROR(#REF! 등)는 null.
 * 수식 셀은 캐시된 결과값으로 판독한다.
 */
final class ExcelCells {

    /** 숫자 비교 허용 오차 — payload 값은 같은 셀에서 추출된 double이라 사실상 동일하나 직렬화 잡음만 흡수한다. */
    private static final double EPSILON = 1e-9;

    /**
     * 행 스캔 종료 기준 — key 컬럼이 모두 빈 행이 연속으로 이만큼 나오면 데이터 영역이 끝난 것으로 본다.
     * (ShippingCost처럼 데이터 앞에 빈 행이 끼는 시트가 있어 첫 빈 행에서 바로 멈추지 않는다)
     */
    private static final int BLANK_STREAK_LIMIT = 20;

    private ExcelCells() {
    }

    /**
     * 행 스캔 결과.
     * matchedRowIndexes는 조건 일치 행, appendRowIndex는 마지막 데이터 행 바로 다음(신규 행 기입 위치)이다.
     */
    record RowScan(List<Integer> matchedRowIndexes, int appendRowIndex) {
    }

    /**
     * startRowIndex부터 key 컬럼 기준으로 데이터 영역을 스캔한다.
     * stop_when_all_key_columns_empty 규칙 — 연속 빈 행이 한도를 넘으면 종료하고,
     * 데이터 행마다 rowMatches 판정으로 일치 행을 수집한다.
     */
    static RowScan scanRows(Sheet sheet, int startRowIndex, List<Integer> keyColumnIndexes, IntPredicate rowMatches) {
        List<Integer> matched = new ArrayList<>();
        int lastDataRowIndex = startRowIndex - 1;
        int blankStreak = 0;
        for (int rowIndex = startRowIndex; blankStreak < BLANK_STREAK_LIMIT; rowIndex++) {
            if (isRowBlank(sheet, rowIndex, keyColumnIndexes)) {
                blankStreak++;
                continue;
            }
            blankStreak = 0;
            lastDataRowIndex = rowIndex;
            if (rowMatches.test(rowIndex)) {
                matched.add(rowIndex);
            }
        }
        return new RowScan(matched, lastDataRowIndex + 1);
    }

    /** 컬럼 문자를 0-based 인덱스로 변환한다. */
    static int columnIndex(String columnLetter) {
        return CellReference.convertColStringToIndex(columnLetter.trim());
    }

    /** 셀 값을 정규화해 읽는다. 값이 없거나 판독 불가(ERROR 등)면 null. */
    static Object readValue(Sheet sheet, int rowIndex, int columnIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            return null;
        }
        return readValue(row.getCell(columnIndex));
    }

    private static Object readValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        CellType type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
        return switch (type) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING -> {
                String text = cell.getStringCellValue().trim();
                yield text.isEmpty() ? null : text;
            }
            case BOOLEAN -> cell.getBooleanCellValue();
            default -> null; // BLANK, ERROR(#REF! 등), _NONE
        };
    }

    /** 지정 컬럼들이 모두 빈 값인 행인지 판정한다. */
    static boolean isRowBlank(Sheet sheet, int rowIndex, List<Integer> columnIndexes) {
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

    /**
     * 셀에 값을 기입한다. 숫자성 값은 숫자 셀로, 그 외는 문자열 셀로 쓴다. 행/셀이 없으면 생성한다.
     * 기입 전에 있던 값을 반환한다 (로그의 이전값 -> 새값 표기용, 없었으면 null).
     */
    static Object writeValue(Sheet sheet, int rowIndex, int columnIndex, Object value) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            row = sheet.createRow(rowIndex);
        }
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            cell = row.createCell(columnIndex);
        }
        Object previous = readValue(cell);
        Double number = asNumber(value);
        if (number != null) {
            cell.setCellValue(number);
        } else {
            cell.setCellValue(String.valueOf(value));
        }
        return previous;
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
