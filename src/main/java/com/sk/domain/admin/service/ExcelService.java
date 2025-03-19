package com.sk.domain.admin.service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sk.domain.admin.dto.request.ExcelReq;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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

  public void uploadExcelFile(MultipartFile file) throws IOException {
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
}