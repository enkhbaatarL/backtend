package com.sk.domain.admin.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.admin.dto.request.DynamicDataReq;
import com.sk.domain.shared.mapper.dynamic.DynamicMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MybatisDynamicQueryService {

    private final DynamicMapper dynamicMapper;
    /**
     * Where 절에서 금지된 토큰 목록
     */
    private static final Set<String> FORBIDDEN_TOKENS = Set.of(";", "--", "#", "UNION");

    public List<Map<String, Object>> getDynamicData(DynamicDataReq dynamicDataReq) {

        String tableName = dynamicDataReq.tableName();
        String columns = dynamicDataReq.columns();
        String whereClause = dynamicDataReq.whereClause();
        String additionalWhereClause = dynamicDataReq.additionalWhereClause();

        // 컬럼 명에서 특수문자 검사 ('*' 미허용)
        if (columns != null) {
            String[] columnArray = columns.split(",");
            for (String col : columnArray) {
                if (!col.trim().matches("^[a-zA-Z0-9_]+$")) {
                    throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "Invalid column name: " + col);
                }
            }
        }

        // where 절에서 금지된 텍스트 검사
        if (whereClause != null) {
            for (String token : FORBIDDEN_TOKENS) {
                if (whereClause.toUpperCase().contains(token)) {
                    throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "Forbidden token found in where clause: " + token);
                }
            }
        }

        Map<String, Object> params = new HashMap<>();
        params.put("columns", columns);
        params.put("tableName", tableName);
        params.put("whereClause", whereClause);
        params.put("additionalWhereClause", additionalWhereClause);
        return dynamicMapper.selectDynamic(params);
    }
}
