package com.sk.domain.admin.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataSaveAll;
import com.sk.domain.shared.entity.master.tcm.ComponentInfo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService {

    private final MybatisDynamicQueryService mybatisDynamicQueryService;
    /**
     * 배치 데이터 저장
     * @param targetDataSource
     * @param columns
     * @param values
     * @param tableName
     * @return
     */
    @Transactional
    public boolean saveBatchData(String targetDataSource, List<String> columns, List<Object[]> values, String tableName) {
        int batchSize = 500;
        int totalRows = values.size();
        for (int i = 0; i < totalRows; i += batchSize) {
            List<Object[]> batchData = values.subList(i, Math.min(i + batchSize, totalRows));
            mybatisDynamicQueryService.saveAllDynamicData(
                DynamicDataSaveAll.of(targetDataSource, tableName, columns, batchData));
        }
        return true;
    }

    /**
     * 컴포넌트 정보 검증
     * @param componentInfo
     */
    public void validateComponentInfo(ComponentInfo componentInfo) {
        if (componentInfo.getTargetDbNm() == null || componentInfo.getTargetDbNm().isBlank()) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Target database name is null or blank");
        }
        if (componentInfo.getExcelTableColumnListNm() == null || componentInfo.getExcelTableColumnListNm().isBlank()) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Column name is null or blank");
        }
    }
}
