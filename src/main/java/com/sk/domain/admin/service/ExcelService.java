package com.sk.domain.admin.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.github.pjfanning.xlsx.StreamingReader;
import com.sk.common.config.database.dataSource.DataSourceContextHolder;
import com.sk.domain.admin.dto.request.ExcelReq;
import com.sk.domain.shared.mapper.dynamic.DynamicMapper;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ExcelService {

    // todo: 시트 2개 이상에서 데이타 담아서 내려주고 읽어오기
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

    private final SqlSessionFactory dynamicSqlSessionFactory;
    private final DynamicMapper dynamicMapper;

    public ExcelService(@Qualifier("dynamicSqlSessionFactory") SqlSessionFactory dynamicSqlSessionFactory,
            DynamicMapper dynamicMapper) {
        this.dynamicSqlSessionFactory = dynamicSqlSessionFactory;
        this.dynamicMapper = dynamicMapper;
    }

    /**
     * 
     * 정형 데이터 엑셀 파일 검수 후 업로드 (Insert 하는 DB가 다를 수 있으니 DataSource 설정 필요 그 후 Mybatis를 사용해서 Insert)
     * 
     * @param file
     * @param columnNames
     * @return
     */
    @Transactional(transactionManager = "dynamicTransactionManager")
    public void validateStructuredExcelData(MultipartFile file, String[] expectedColumnNames, String tableName, String tenantKey) throws IOException {

        // 테넌트 설정
        DataSourceContextHolder.setDataSourceKey(tenantKey);

        try (InputStream is = file.getInputStream();
                Workbook workbook = StreamingReader.builder()
                        .rowCacheSize(100)
                        .bufferSize(4096)
                        .open(is);
                SqlSession sqlSession = dynamicSqlSessionFactory.openSession(ExecutorType.BATCH)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            // 1. 헤더 검증
            if (!rowIterator.hasNext()) {
                throw new IllegalArgumentException("Excel file is empty");
            }
            Row headerRow = rowIterator.next();
            List<String> actualColumns = extractColumnNames(headerRow);
            validateColumns(actualColumns, expectedColumnNames);

            // 2. 칼럼명 문자열 생성
            String columns = String.join(", ", expectedColumnNames);

            // 3. 스트림으로 행 처리
            Stream<Row> rowStream = Stream.generate(() -> rowIterator.hasNext() ? rowIterator.next() : null)
                    .takeWhile(Objects::nonNull)
                    .filter(row -> row.getLastCellNum() > 0);

            int batchCount = 0;
            DynamicMapper mapper = sqlSession.getMapper(DynamicMapper.class);

            // 4. 배치 삽입
            for (Iterator<Row> iterator = rowStream.iterator(); iterator.hasNext();) {
                Row row = iterator.next();
                Map<String, String> params = new HashMap<>();
                params.put("tableName", tableName);
                params.put("columns", columns);
                params.put("values", parseRowToValues(row, expectedColumnNames));

                mapper.insertDynamic(params);
                batchCount++;

                if (batchCount % 1000 == 0) {
                    sqlSession.flushStatements();
                    sqlSession.commit();
                }
            }
            if (batchCount % 1000 != 0) {
                sqlSession.flushStatements();
                sqlSession.commit();
            }
        } finally {
            DataSourceContextHolder.clearDataSourceKey(); // ThreadLocal 정리
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

        if (!actualSet.equals(expectedSet)) {
            List<String> missing = expectedSet.stream().filter(col -> !actualSet.contains(col)).collect(Collectors.toList());
            List<String> extra = actualSet.stream().filter(col -> !expectedSet.contains(col)).collect(Collectors.toList());
            String errorMessage = "Column mismatch: " +
                    (missing.isEmpty() ? "" : "Missing: " + missing) +
                    (extra.isEmpty() ? "" : "; Extra: " + extra);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * 행 데이터 파싱
     * 
     * @param row
     * @param columnNames
     * @return
     */
    private String parseRowToValues(Row row, String[] columnNames) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < columnNames.length; i++) {
            Cell cell = row.getCell(i);
            String value = cell != null ? cell.getStringCellValue().trim() : "";
            value = "'" + value.replace("'", "''") + "'"; // SQL 인젝션 방지
            values.add(value);
        }
        return String.join(", ", values);
    }
}