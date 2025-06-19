package com.sk.common.utils;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.SystemErrorException;
import com.sk.common.model.TableRes;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.IntStream;


@Slf4j
public class ExcelUtils {

    private static final Pattern INVALID_SHEET_NAME_PATTERN = Pattern.compile("[:\\\\/?*\\[\\]]");

    public static ByteArrayInputStream toExcel(String title, List<TableRes.Column> columns, List<TableRes.Row> rows) {

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
