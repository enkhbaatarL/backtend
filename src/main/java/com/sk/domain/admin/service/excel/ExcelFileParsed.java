package com.sk.domain.admin.service.excel;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.web.multipart.MultipartFile;

import com.sk.common.utils.ExcelParsedUtil;
import com.sk.domain.shared.entity.master.tcm.ComponentInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ExcelFileParsed implements FileTypeStrategy {
    private final DataCollectorStrategyResolver dataCollectorStrategyResolver;

    /**
     * 엑셀 파일 파싱
     * 정형 데이터 엑셀 파일 검수 후 데이터 저장
     * 현재 정책 : 엑셀 1행 => 칼럼, 2행부터 데이터 저장
     * => 필요에 따라 조건이 들어갈 수 있음 (몇번째가 칼럼이고, 몇번째가 데이터인지 조건)
     * 엑셀의 시트는 한개만 사용
     *
     * 테이블 칼럼 리스트(그래프를 그리기 위한 칼럼, tableColumnListNm)와, 엑셀 칼럼 리스트(excelColumnListNm) 명이 다를 수 있음
     * 
     * @param file 엑셀 파일
     * @param componentInfo 컴포넌트 정보
     * @param headerRowIndex 헤더 행 인덱스 (기본값 0)
     * @param dataStartRowIndex 데이터 시작 행 인덱스 (기본값 1)
     * @param criteriaDt 조건 일자
     * @return 파싱된 데이터
     * @throws IOException 파일 처리 중 오류 발생 시
     */
    @Override
    public List<Object[]> processFile(MultipartFile file, ComponentInfo componentInfo, int headerRowIndex, int dataStartRowIndex, String criteriaDt) throws IOException {
        Workbook workbook = ExcelParsedUtil.createWorkbook(file);
        try {
            Sheet sheet = workbook.getSheetAt(0); // 첫번째 시트
            DataCollectorStrategy strategy = dataCollectorStrategyResolver.resolve(componentInfo.getDataStructureConvertNm());
            List<Object[]> result = strategy.processData(sheet, Arrays.asList(componentInfo.getExcelTableColumnListNm().split(",")), criteriaDt);
            workbook.close();
            return result;
        } catch (IOException e) {
            workbook.close();
            throw e;
        }
    }
}
