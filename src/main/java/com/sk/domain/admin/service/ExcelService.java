package com.sk.domain.admin.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.github.pjfanning.xlsx.StreamingReader;
import com.sk.common.config.database.dataSource.DataSourceContextHolder;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.admin.dto.request.ExcelReq;
import com.sk.domain.shared.entity.master.tcm.ComponentInfo;
import com.sk.domain.shared.mapper.dynamic.DynamicMapper;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ExcelService {

    private final SqlSessionFactory sqlSessionFactory;
    private final int DEFAULT_HEADER_ROW_INDEX = 0; // 기본 엑셀 헤더 행 인덱스 (파일에서 1행 => 인덱스 0)
    private final int DEFAULT_DATA_START_ROW_INDEX = 1; // 기본 엑셀 데이터 시작 행 인덱스 (파일에서 2행 => 인덱스 1)
    private final int MAX_CONSECUTIVE_EMPTY_ROWS = 5; // 최대 허용 빈 행 수 (엑셀 검수 시 연속 빈 행 수가 5개 이상이면 종료)

    @Autowired
    public ExcelService(@Qualifier("dynamicSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    // todo: 시트 2개 이상에서 데이타 담아서 내려주고 읽어오기
    /**
     * 엑셀 파일 다운로드
     * 
     * @param response
     * @param datas
     * @return
     * @throws IOException
     */
    public ByteArrayResource downloadExcelFile(HttpServletResponse response, List<List<ExcelReq>> datas)
            throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {

            int sheetIndex = 0;
            for (List<ExcelReq> list : datas) {
                String sheetName = String.format("SheetName_%d", ++sheetIndex);

                Sheet sheet = workbook.createSheet(sheetName);

                Row headerRow = (Row) sheet.createRow(0);
                Cell headerCell1 = headerRow.createCell(0);
                headerCell1.setCellValue("이름");

                Cell headerCell2 = headerRow.createCell(1);
                headerCell2.setCellValue("레벨");

                IntStream.range(0, list.size())
                        .forEach(x -> {
                            ExcelReq data = list.get(x);

                            Row dataRow1 = sheet.createRow(x + 1);
                            Cell dataCell1 = dataRow1.createCell(0);
                            dataCell1.setCellValue(data.getName());

                            Cell dataCell2 = dataRow1.createCell(1);
                            dataCell2.setCellValue(data.getGrade());
                        });
            }
            String filename = "sample.xlsx";

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"" +
                    filename + "\"");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            workbook.close();

            return new ByteArrayResource(out.toByteArray());
        }
    }

    /**
     * 엑셀 파일 업로드
     * 
     * @param file
     * @throws IOException
     */
    public void uploadExcelFile(MultipartFile file) throws IOException {
        // HSSFWorkbook
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            int sheetCount = workbook.getNumberOfSheets();
            for (int i = 0; i < sheetCount; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) {
                        continue;
                    }

                    // 엑셀의 A열(0번 셀) = name, B열(1번 셀) = grade 라고 가정
                    Cell nameCell = row.getCell(0);
                    Cell gradeCell = row.getCell(1);

                    log.info("-> {}, {}", nameCell, gradeCell);

                    String name = (nameCell == null) ? "" : nameCell.getStringCellValue();
                    String grade = (gradeCell == null) ? null : gradeCell.getStringCellValue();

                    log.info("{}, {}", name, grade);
                }
            }

        } catch (

        IOException e) {
            log.error("IOException", e);
            // 파일 처리를 실패한 경우
            // return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            // .body("엑셀 파일을 읽는 도중 오류가 발생했습니다: " + e.getMessage());
        } catch (Exception e) {
            log.error("Exception", e);
            // 그 외 처리에서 발생할 수 있는 예외
            // return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            // .body("엑셀 데이터 처리 중 예외가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 컴포넌트 정보에 따른 정형 데이터 저장
     * 
     * @param file
     * @param componentInfo
     * @return
     * @throws IOException
     */
    public boolean uploadStructuredData(MultipartFile file, ComponentInfo componentInfo) throws IOException {
        if(componentInfo.getTargetDbNm() == null || componentInfo.getTargetDbNm().isBlank()) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Target database name is null or blank");
        }
        try {
            // 데이터 소스 설정
            DataSourceContextHolder.setDataSourceKey(componentInfo.getTargetDbNm());

            // 컴포넌트 엑셀 칼럼 추출
            String columnName = componentInfo.getExcelColumnListNm();

            if (columnName == null || columnName.isBlank()) {
                throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Column name is null or blank");
            }

            String[] columnNames = Arrays.stream(columnName.split(",")).map(String::trim).filter(name -> !name.isEmpty()).toArray(String[]::new);

            int headerRowIndex = DEFAULT_HEADER_ROW_INDEX;
            int dataStartRowIndex = DEFAULT_DATA_START_ROW_INDEX;

            // 컴포넌트 조건절에 HEADER_ROW_INDEX, DATA_START_ROW_INDEX 값 있는지 확인
            // 조건절 형식 : HEADER_ROW_INDEX=1;DATA_START_ROW_INDEX=2 (엑셀파일의 1행이면 )
            //          : 입력 예시 : HEADER_ROW_INDEX=0;DATA_START_ROW_INDEX=1;
            if(componentInfo.getExcelConditionCntt() != null && !componentInfo.getExcelConditionCntt().isBlank()) {
                String[] condition = componentInfo.getExcelConditionCntt().split(";");
                for(String cond : condition) {
                    if(cond.contains("HEADER_ROW_INDEX") || cond.contains("DATA_START_ROW_INDEX")) {
                        String[] keyValue = cond.split("=");
                        if(keyValue[0].equals("HEADER_ROW_INDEX")) {
                            int conditionHeaderRowIndex = Integer.parseInt(keyValue[1]); // 엑셀파일의 1행이면 0행
                            if(conditionHeaderRowIndex <= 0) {
                                headerRowIndex = 0; // headerRowIndex의 기본값은 0
                            } else {
                                headerRowIndex = conditionHeaderRowIndex - 1;
                            }
                        }
                        if(keyValue[0].equals("DATA_START_ROW_INDEX")) {
                            int conditionDataStartRowIndex = Integer.parseInt(keyValue[1]); // 엑셀파일의 2행이면 1행
                            if(conditionDataStartRowIndex <= 1) {
                                dataStartRowIndex = 1; // dataStartRowIndex의 기본값은 1
                            } else {
                                dataStartRowIndex = conditionDataStartRowIndex - 1;
                            }
                        }
                    }
                }
            }

            return uploadStructuredExcelData(file, columnNames, componentInfo.getTargetTableNm(), headerRowIndex, dataStartRowIndex);

        } catch (Exception e) {
            log.error("Exception", e);
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, e.getMessage());
        } finally {
            DataSourceContextHolder.clearDataSourceKey();
        }
    }

    /**
     * 정형 데이터 엑셀 파일 검수 후 데이터 저장
     * 현재 정책 : 엑셀 1행 => 칼럼, 2행부터 데이터 저장
     *          => 필요에 따라 조건이 들어갈 수 있음 (몇번째가 칼럼이고, 몇번째가 데이터인지 조건)
     *          엑셀의 시트는 한개만 사용
     * 
     * @param file 엑셀 파일
     * @param expectedColumnNames 기대되는 칼럼명
     * @param tableName 테이블 이름
     * @param headerRowIndex 헤더 행 인덱스 (기본값 0)
     * @param dataStartRowIndex 데이터 시작 행 인덱스 (기본값 1)
     * @return 저장 성공 여부
     * @throws IOException 파일 처리 중 오류 발생 시
     */
    public boolean uploadStructuredExcelData(MultipartFile file, String[] expectedColumnNames, String tableName, int headerRowIndex, int dataStartRowIndex) {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            try {
                // 1. 엑셀 구조 검증
                List<String> actualColumns = validateExcelStructure(file, expectedColumnNames, headerRowIndex);

                // 2. 데이터 수집
                List<Object[]> values = collectExcelData(file, actualColumns, dataStartRowIndex);

                // 3. DB에 배치 삽입 (배치 사이즈 500개씩 saveAll)
                DynamicMapper mapper = sqlSession.getMapper(DynamicMapper.class);
                int batchSize = 500; // 500개씩 처리
                int totalRows = values.size();
                int batchCount = (totalRows + batchSize - 1) / batchSize; // 총 배치 수 (올림 처리)

                for (int i = 0; i < batchCount; i++) {
                    int fromIndex = i * batchSize;
                    int toIndex = Math.min(fromIndex + batchSize, totalRows);
                    List<Object[]> batchData = values.subList(fromIndex, toIndex);

                    log.info("Batch {}: fromIndex={}, toIndex={}, size={}, first element: {}", 
                        i + 1, fromIndex, toIndex, batchData.size(), batchData.isEmpty() ? "empty" : batchData.get(0).getClass());

                    Map<String, Object> param = new HashMap<>();
                    param.put("tableName", tableName);
                    param.put("columns", actualColumns);
                    param.put("values", batchData);
                    mapper.saveAllDynamic(param);

                    log.info("Processed batch {} of {}: {} rows to {}", i + 1, batchCount, batchData.size(), tableName);
                }

                sqlSession.commit(); // 모든 배치 성공 시 커밋
                log.info("Committed total {} rows to {}", totalRows, tableName);
                return true;
            } catch (Exception e) {
                log.error("Exception during upload: {}", e.getMessage(), e);
                sqlSession.rollback();
                throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, e.getMessage());
            }
        } 
    }

    /**
     * 엑셀 파일의 구조를 검증하고 헤더 칼럼을 반환
     *
     * @param file 업로드된 엑셀 파일
     * @param expectedColumnNames 기대되는 칼럼명
     * @return 실제 칼럼 이름 리스트
     * @throws IOException 파일 처리 중 오류 발생 시
     */
    public List<String> validateExcelStructure(MultipartFile file, String[] expectedColumnNames, int headerRowIndex) {
        try (InputStream is = file.getInputStream();
                Workbook workbook = StreamingReader.builder()
                        .rowCacheSize(100)
                        .bufferSize(4096)
                        .open(is)) {

            // 1. 엑셀 확장가 파일 검증
            validateExcelFile(file);

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            // 1. 엑셀이 비어있는지 확인
            if (!rowIterator.hasNext()) {
                throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Excel file is empty");
            }

            // 2. 헤더 행까지 이동
            Row headerRow = null;
            for (int i = 0; i <= headerRowIndex && rowIterator.hasNext(); i++) {
                headerRow = rowIterator.next();
            }
            if (headerRow == null) {
                throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Header row not found at index " + headerRowIndex);
            }
            
            List<String> actualColumns = extractColumnNames(headerRow);

            // 3. 기대 칼럼과 실제 칼럼 검증
            validateColumns(actualColumns, expectedColumnNames);

            /**
             * 나머지 행 데이터 수집(데이터 검증 로직, 검증만 필요시 행 데이터를 검증하는 로직 사용)
                List<Map<String, String>> data = new ArrayList<>();
                while (rowIterator.hasNext()) { //TODO: 끊어진 행이 있으면 문제 발생 함 => 처리 필요
                    Row row = rowIterator.next();
                    if (row.getLastCellNum() > 0) { // 빈 행 제외
                        Map<String, String> rowData = parseRowToMap(row, actualColumns);
                        data.add(rowData);
                    }
                }
                return data; // 모든 데이터 반환
             */

            return actualColumns;
        } catch (Exception e) {
            log.error("Exception", e);
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, e.getMessage());
        }
    }

    /**
     * 엑셀 파일에서 데이터를 수집
     * 현 정책은 시트는 1개라고 가정
     *
     * @param file 업로드된 엑셀 파일
     * @param actualColumns 헤더 칼럼 이름 리스트
     * @return 수집된 데이터
     * @throws IOException 파일 처리 중 오류 발생 시
     */
    private List<Object[]> collectExcelData(MultipartFile file, List<String> actualColumns, int dataStartRowIndex) {
        try (InputStream is = file.getInputStream();
            Workbook workbook = StreamingReader.builder()
                    .rowCacheSize(100)
                    .bufferSize(4096)
                    .open(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            // 데이터 시작 행까지 이동
            for (int i = 0; i < dataStartRowIndex && rowIterator.hasNext(); i++) {
                rowIterator.next();
            }

            // 데이터 수집
            List<Object[]> data = new ArrayList<>();
            int consecutiveEmptyRows = 0; // 연속 빈 행 수

            // 5행 이상 연속 빈 행이면 종료
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (row.getLastCellNum() > 0) { // 데이터가 있는 행
                    Object[] rowData = parseRowToArray(row, actualColumns);
                    if (rowData.length == 0) {
                        log.warn("Parsed empty row data for row: {}", row.getRowNum());
                    } else {
                        data.add(rowData);
                        consecutiveEmptyRows = 0; // 데이터가 있으면 카운터 초기화
                    }
                } else {
                    // 빈 행 처리
                    consecutiveEmptyRows++;
                    log.debug("Empty row detected at row: {}, consecutive empty rows: {}", row.getRowNum(), consecutiveEmptyRows);
                    if (consecutiveEmptyRows >= MAX_CONSECUTIVE_EMPTY_ROWS) {
                        log.info("Terminating data collection: {} consecutive empty rows reached", MAX_CONSECUTIVE_EMPTY_ROWS);

                    }
                }
            }

            return data;
        } catch (Exception e) {
            log.error("Exception during data collection: {}", e.getMessage(), e);
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, e.getMessage());
        }
    }

    /**
    * 업로드된 파일이 엑셀 형식인지 검증
    * 
    * @param file 업로드된 파일
    * @throws AppErrorException 엑셀 파일이 아닌 경우
    */
    private void validateExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "File is null or empty");
        }

        String fileName = file.getOriginalFilename();
        String contentType = file.getContentType();

        // 파일 이름이 null이거나 확장자를 확인할 수 없는 경우
        if (fileName == null || !fileName.matches("(?i).+\\.(xlsx|xls)$")) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID,
                    "Invalid file extension. Only .xlsx and .xls are allowed. File: " + fileName);
        }

        // MIME 타입 확인
        if (contentType == null ||
                !contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") && // .xlsx
                        !contentType.equals("application/vnd.ms-excel")) { // .xls
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID,
                    "Invalid file type. Expected Excel file, but got: " + contentType);
        }
    }

    /**
     * 헤더 행에서 칼럼명 추출
     * 
     * @param headerRow
     * @return
     */
    private List<String> extractColumnNames(Row headerRow) {
        List<String> columnNames = new ArrayList<>();
        for (Cell cell : headerRow) {
            columnNames.add(cell.getStringCellValue().trim());
        }
        return columnNames;
    }

    /**
     * 칼럼명 검증
     * 
     * @param actualColumns
     * @param expectedColumns
     */
    private void validateColumns(List<String> actualColumns, String[] expectedColumns) {
        Set<String> expectedSet = Arrays.stream(expectedColumns).map(String::trim).collect(Collectors.toSet());
        Set<String> actualSet = new HashSet<>(actualColumns);

        // actualColumns에만 있는 칼럼(엑셀 칼럼에만 있는 경우) 체크
        List<String> extra = actualSet.stream()
        .filter(col -> !expectedSet.contains(col))
        .collect(Collectors.toList());

        if (!extra.isEmpty()) {
            String errorMessage = "Column mismatch: Extra columns found in Excel: " + extra;
            throw AppErrorException.of(ResultCode.Error.BIZ_VALIDATION_FAILED, errorMessage);
        }
    }

    /**
     * 행 데이터 파싱
     * 
     * @param row
     * @param columnNames
     * @return
     */
    private Object[] parseRowToArray(Row row, List<String> columnNames) {
        Object[] rowData = new Object[columnNames.size()];
        for (int i = 0; i < columnNames.size(); i++) {
            Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            Object value = null;
            
            if (cell == null || cell.getCellType() == CellType.BLANK) {
                value = null;
            } else {
                switch (cell.getCellType()) {
                    case STRING:
                        value = cell.getStringCellValue(); // VARCHAR
                        break;
                    case NUMERIC:
                        if (DateUtil.isCellDateFormatted(cell)) {
                            value = cell.getDateCellValue(); // DATETIME
                        } else {
                            double numValue = cell.getNumericCellValue();
                            if (numValue == Math.floor(numValue)) {
                                value = (int) numValue; // INT (소수점 없으면)
                            } else {
                                value = BigDecimal.valueOf(numValue); // DECIMAL (소수점 있으면)
                            }
                        }
                        break;
                    case BOOLEAN:
                        value = cell.getBooleanCellValue(); // DB에 BOOLEAN 매핑 가능
                        break;
                    default:
                        value = cell.toString(); // 기본 VARCHAR
                }

            }
            rowData[i] = value;
        }
        return rowData;
    }
}
