package com.sk.common.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.csv.CSVRecord;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.web.multipart.MultipartFile;

import com.github.pjfanning.xlsx.StreamingReader;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExcelParsedUtil {
    /**
     * 엑셀 파일 생성
     * @param file
     * @return
     * @throws IOException
     */
    public static Workbook createWorkbook(MultipartFile file) throws IOException {
        String fileName = validateFile(file);
        // 26.07.29 취약점 진단 검토, '표준 라이브러리의 널 반환 값 역참조-102111,102112,102113', 오탐 -> MultipartFile.getInputStream()은 null을 반환하지 않는 계약(실패 시 IOException)이며 file은 validateFile에서 null/empty 검증됨.
        InputStream inputStream = file.getInputStream();
        if (fileName.endsWith(".xlsx")) {
            Workbook workbook = StreamingReader.builder().rowCacheSize(100).bufferSize(4096).open(inputStream);
            inputStream.close();
            return workbook;
        } else if (fileName.endsWith(".xls")) {
            Workbook workbook = new HSSFWorkbook(inputStream);
            inputStream.close();
            return workbook;
        }
        inputStream.close();
        throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "Unsupported Excel file type: " + fileName);
    }

    /**
     * 파일 유효성 검사
     * @param file
     * @return
     */
    public static String validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "File is null or empty");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.matches("(?i).+\\.(xlsx|xls|csv)$")) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "Invalid file extension: " + fileName);
        }
        return fileName.toLowerCase();
    }

    /**
     * 구분자 감지
     * @param file
     * @return
     * @throws IOException
     */
    public static char detectDelimiter(MultipartFile file) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
        String firstLine = reader.readLine();
        reader.close();
        if (firstLine == null)
            return ',';
        firstLine = firstLine.replaceAll("^[\\uFEFF\\u200B]", "");
        int commaCount = firstLine.split(",").length;
        int semicolonCount = firstLine.split(";").length;
        int tabCount = firstLine.split("\t").length;
        if (semicolonCount > commaCount && semicolonCount > tabCount)
            return ';';
        if (tabCount > commaCount && tabCount > semicolonCount)
            return '\t';
        return ',';
    }

    /**
     * 엑셀 헤더 추출
     * @param sheet
     * @param headerRowIndex
     * @return
     */
    public static List<String> extractExcelHeader(Sheet sheet, int headerRowIndex) {
        List<String> headers = new ArrayList<>();

        for (Cell cell : getHeaderRow(sheet, headerRowIndex)) {
            String value = cell.getStringCellValue() != null ? cell.getStringCellValue().trim() : null;
            headers.add(value);
        }
        return headers;
    }

    private static Row getHeaderRow(Sheet sheet, int headerRowIndex){
        List<String> headers = new ArrayList<>();
        Iterator<Row> rowIterator = sheet.iterator();
        // 헤더 행까지 이동
        Row headerRow = null;
        while (rowIterator.hasNext()) {
            headerRow = rowIterator.next();
            if(headerRow.getRowNum() >= headerRowIndex) {
                break;
            }
        }

        if (headerRow == null) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Header row not found at index " + headerRowIndex);
        }

        return headerRow;
    }

    public static List<LocalDate> extractExcelDateHeader(Sheet sheet, int headerRowIndex) {
        List<LocalDate> headers = new ArrayList<>();

        for (Cell cell : getHeaderRow(sheet, headerRowIndex)) {
            Date value = cell.getDateCellValue() != null ? cell.getDateCellValue() : null;
            if(value == null){
                headers.add(null);
                continue;
            }
            headers.add(value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        }
        return headers;
    }

    /**
     * 엑셀 행 파싱
     * @param row
     * @param columnNames
     * @param criteriaDt
     * @return
     */
    public static Object[] parseExcelRow(Row row, List<String> columnNames, String criteriaDt) {
        List<Object> rowData = new ArrayList<>();
        if (criteriaDt != null) {
            rowData.add(criteriaDt);
        }
        for (int i = 0; i < columnNames.size(); i++) {
            Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            Object value = null;
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                switch (cell.getCellType()) {
                    case STRING:
                        value = cell.getStringCellValue();
                        break;
                    case NUMERIC:
                        if (DateUtil.isCellDateFormatted(cell)) {
                            value = cell.getDateCellValue();
                        } else {
                            double numValue = cell.getNumericCellValue();
                            if (numValue == Math.floor(numValue)) {
                                value = (int) numValue;
                            } else {
                                value = BigDecimal.valueOf(numValue);
                            }
                        }
                        break;
                    case BOOLEAN:
                        value = cell.getBooleanCellValue();
                        break;
                    default:
                        value = cell.toString();
                }
            }
            rowData.add(value);
        }
        return rowData.toArray();
    }

    /**
     * CSV 레코드 파싱
     * @param record
     * @param columnNames
     * @param criteriaDt
     * @return
     */
    public static Object[] parseCsvRecord(CSVRecord record, List<String> columnNames, String criteriaDt) {
        List<Object> rowData = new ArrayList<>();
        for (int i = 0; i < columnNames.size(); i++) {
            String value = i < record.size() ? record.get(i) : null;
            rowData.add(value != null && !value.trim().isEmpty() ? value.trim() : null);
        }
        if (criteriaDt != null) {
            rowData.add(criteriaDt);
        }
        return rowData.toArray();
    }
}
