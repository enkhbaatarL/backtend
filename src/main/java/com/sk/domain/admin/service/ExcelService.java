package com.sk.domain.admin.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.sk.common.config.database.DataSourceContextHolder;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.ExcelParsedUtil;
import com.sk.domain.admin.dto.request.ExcelReq;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataSaveAll;
import com.sk.domain.admin.service.excel.FileTypeStrategy;
import com.sk.domain.admin.service.excel.FileTypeStrategyResolver;
import com.sk.domain.shared.entity.master.tcm.ComponentInfo;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelService {

    private static final int DEFAULT_HEADER_ROW_INDEX = 0;
    private static final int DEFAULT_DATA_START_ROW_INDEX = 1;
    private static final String CRITERIA_DT_COLUMN_NAME = "crtr_dt";

    private final MybatisDynamicQueryService mybatisDynamicQueryService;
    private final FileTypeStrategyResolver fileTypeStrategyResolver;

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
     * 테이블 칼럼 리스트(그래프를 그리기 위한 칼럼, tableColumnListNm)와, 엑셀 칼럼 리스트(excelColumnListNm) 명이 다를 수 있음
     * 칼럼명은 서로 1:1 대응 된다는 가정으로 작업하며 테이블 칼럼 리스트(tableColumnListNm) 순서와 엑셀 칼럼 리스트(excelColumnListNm) 순서가 같아야 함
     *
     * @param file
     * @param componentInfo
     * @param criteriaDt 기준일자 (엑셀에는 없지만 DB 테이블에는 저장이 필요할 때 따로 받는다, 현재는 crtr_dt 칼럼에만 저장한다)
     * @return
     * @throws IOException
     */
    @Transactional
    public boolean uploadStructuredData(MultipartFile file, ComponentInfo componentInfo, String criteriaDt, String category) throws IOException {
        try {
            validateComponentInfo(componentInfo);
            List<String> columnNames = Arrays.stream(componentInfo.getExcelTableColumnListNm().split(","))
                    .map(String::trim)
                    .filter(name -> !name.isEmpty())
                    .collect(Collectors.toList());

            int headerRowIndex = DEFAULT_HEADER_ROW_INDEX;
            int dataStartRowIndex = DEFAULT_DATA_START_ROW_INDEX;

            if (componentInfo.getExcelConditionCntt() != null && !componentInfo.getExcelConditionCntt().isBlank()) {
                String[] conditions = componentInfo.getExcelConditionCntt().split(";");
                for (String cond : conditions) {
                    String[] keyValue = cond.split("=");
                    if (keyValue[0].equals("HEADER_ROW_INDEX")) {
                        headerRowIndex = Math.max(0, Integer.parseInt(keyValue[1]) - 1);
                    } else if (keyValue[0].equals("DATA_START_ROW_INDEX")) {
                        dataStartRowIndex = Math.max(1, Integer.parseInt(keyValue[1]) - 1);
                    }
                }
            }

            FileTypeStrategy strategy = fileTypeStrategyResolver.resolve(ExcelParsedUtil.validateFile(file));
            List<Object[]> values = strategy.processFile(file, componentInfo, headerRowIndex, dataStartRowIndex, criteriaDt, category);

            if (criteriaDt != null) {
                columnNames.add(CRITERIA_DT_COLUMN_NAME);
            }

            return saveBatchData(componentInfo.getTargetDbNm(), columnNames, values, componentInfo.getTargetTableNm());
        } catch (Exception e) {
            log.error("Upload failed: {}", e.getMessage(), e);
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, e.getMessage());
        } finally {
            DataSourceContextHolder.clearDataSourceKey();
        }
    }

    /**
     * 컴포넌트 정보 검증
     * @param componentInfo
     */
    private void validateComponentInfo(ComponentInfo componentInfo) {
        if (componentInfo.getTargetDbNm() == null || componentInfo.getTargetDbNm().isBlank()) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Target database name is null or blank");
        }
        if (componentInfo.getExcelTableColumnListNm() == null || componentInfo.getExcelTableColumnListNm().isBlank()) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Column name is null or blank");
        }
    }

    /**
     * 배치 데이터 저장
     * @param targetDataSource
     * @param columns
     * @param values
     * @param tableName
     * @return
     */
    private boolean saveBatchData(String targetDataSource, List<String> columns, List<Object[]> values, String tableName) {
        int batchSize = 500;
        int totalRows = values.size();
        for (int i = 0; i < totalRows; i += batchSize) {
            List<Object[]> batchData = values.subList(i, Math.min(i + batchSize, totalRows));
            mybatisDynamicQueryService.saveAllDynamicData(DynamicDataSaveAll.of(targetDataSource, tableName, columns, batchData));
        }
        return true;
    }
}
