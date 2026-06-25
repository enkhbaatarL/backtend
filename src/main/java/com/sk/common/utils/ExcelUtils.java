package com.sk.common.utils;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.SystemErrorException;
import com.sk.common.model.TableRes;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;


@Slf4j
public class ExcelUtils {

    private static final Pattern INVALID_SHEET_NAME_PATTERN = Pattern.compile("[:\\\\/?*\\[\\]]");

    private ExcelUtils() {
    }

    public static ByteArrayInputStream toExcel(String title, List<TableRes.Column> columns, List<TableRes.Row> rows) {

        return new ByteArrayInputStream(toExcelBytes(title, columns, rows));
    }

    public static byte[] toExcelBytes(String title, List<TableRes.Column> columns, List<TableRes.Row> rows) {

        String[] headers = columns.stream().map(TableRes.Column::name).toArray(String[]::new);

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(sanitizeSheetName(title));

            // Header
            Row headerRow = sheet.createRow(0);
            for (int col = 0; col < headers.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(headers[col]);
            }

            // Data
            IntStream.range(0, rows.size()).forEach(rowIdx -> {
                TableRes.Row rowData = rows.get(rowIdx);
                Row row = sheet.createRow(rowIdx + 1);

                List<String> cells = rowData.cells();
                IntStream.range(0, cells.size()).forEach(cellIdx -> {
                    row.createCell(cellIdx).setCellValue(cells.get(cellIdx));
                });

            });

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Excel download failed: {}", e.getMessage(), e);
            throw SystemErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "excel download fail.");
        }

    }

    /**
     * 다중 시트 엑셀 생성. key = 시트명, value = 해당 시트의 표(headers + rows).
     * 셀 값이 null 이면 빈 칸으로 둔다. (기존 단일 시트 toExcel 과 독립된 메서드)
     */
    public static ByteArrayInputStream toExcel(LinkedHashMap<String, TableRes> sheets) {

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            sheets.forEach((name, table) -> {
                Sheet sheet = workbook.createSheet(sanitizeSheetName(name));

                List<TableRes.Column> columns = table.headers();
                Row headerRow = sheet.createRow(0);
                for (int col = 0; col < columns.size(); col++) {
                    headerRow.createCell(col).setCellValue(columns.get(col).name());
                }

                List<TableRes.Row> rows = table.rows();
                IntStream.range(0, rows.size()).forEach(rowIdx -> {
                    List<String> cells = rows.get(rowIdx).cells();
                    Row row = sheet.createRow(rowIdx + 1);
                    IntStream.range(0, cells.size()).forEach(cellIdx -> {
                        String value = cells.get(cellIdx);
                        if (value != null) {
                            row.createCell(cellIdx).setCellValue(value);
                        }
                    });
                });
            });

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            log.error("Excel download failed: {}", e.getMessage(), e);
            throw SystemErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "excel download fail.");
        }
    }

    private static String sanitizeSheetName(String name) {
        String cleaned = INVALID_SHEET_NAME_PATTERN.matcher(name).replaceAll("_");
        return cleaned.length() > 31 ? cleaned.substring(0, 31) : cleaned;
    }

}
