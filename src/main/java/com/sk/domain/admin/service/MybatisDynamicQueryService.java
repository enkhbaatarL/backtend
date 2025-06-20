package com.sk.domain.admin.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sk.common.config.redis.CacheType;
import com.sk.common.utils.CombineUtil;
import com.sk.domain.shared.mapper.dto.DoubleValueResultData;
import com.sk.domain.shared.mapper.dto.SchemaMetadata;
import com.sk.domain.shared.mapper.dto.TargetPredictResultData;
import com.sk.domain.shared.mapper.dynamic.MetadataMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.sk.common.config.database.DataSourceContextHolder;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataReq;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataSaveAll;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataDetailReq;
import com.sk.domain.shared.mapper.dynamic.DynamicMapper;

import org.springframework.security.core.context.SecurityContextHolder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MybatisDynamicQueryService {

    private final DynamicMapper dynamicMapper;
    private final MetadataMapper metadataMapper;

    /**
     * Where 절에서 금지된 토큰 목록
     */
    private static final Set<String> FORBIDDEN_TOKENS = Set.of(";", "--", "#", "UNION");

    public List<Map<String, Object>> getDynamicData(DynamicDataReq dynamicDataReq) {

        // 데이터 소스 설정
        DataSourceContextHolder.setDataSourceKey(dynamicDataReq.targetDataSource());

        String tableName = dynamicDataReq.tableName();
        String columns = dynamicDataReq.columns();
        String whereClause = dynamicDataReq.whereClause();
        String additionalWhereClause = dynamicDataReq.additionalWhereClause();

        // 컬럼 명에서 특수문자 검사 ('*' 미허용)
        if (columns != null) {
            String[] columnArray = columns.split(",");
            for (String col : columnArray) {
                if (col.trim().equalsIgnoreCase("count(*)")) continue; // count 쿼리 허용
                if (col.trim().contains("max")) continue; // max 쿼리 허용
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
        params.put("orderByColumn", dynamicDataReq.orderByColumn());
        params.put("orderByDirection", dynamicDataReq.orderByDirection());
        params.put("limit", dynamicDataReq.limit());
        params.put("offset", dynamicDataReq.offset());
        return dynamicMapper.selectDynamic(params);
    }

    /**
     * 정형 데이터 저장
     * @param dynamicDataSaveAll
     */
    public void saveAllDynamicData(DynamicDataSaveAll dynamicDataSaveAll) {
        // 데이터 소스 설정
        DataSourceContextHolder.setDataSourceKey(dynamicDataSaveAll.tartgetDataSource());
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        String tableName = dynamicDataSaveAll.tableName();
        List<String> columns = dynamicDataSaveAll.columns();
        List<Object[]> values = dynamicDataSaveAll.values();

        Map<String, Object> params = new HashMap<>();
        params.put("tableName", tableName);
        params.put("columns", columns);
        params.put("values", values);
        params.put("currentUserId", currentUserId != null ? currentUserId : "SYSTEM");
        dynamicMapper.saveAllDynamic(params);
    }

    /**
     * 정형 데이터 조회 서브쿼리도 함께 조회 하기 때문에 Split후에 검사 하지 않는다
     * @param dynamicDataReq
     * @return
     */
    public List<Map<String, Object>> getDynamicRawQuery(DynamicDataDetailReq dynamicDataDetailReq) {
        // 데이터 소스 설정
        DataSourceContextHolder.setDataSourceKey(dynamicDataDetailReq.targetDataSource());

        String tableName = dynamicDataDetailReq.tableName();
        String columns = dynamicDataDetailReq.columns();
        String whereClause = dynamicDataDetailReq.whereClause();
        String orderByColumns = dynamicDataDetailReq.orderByColumns();
        String groupByColumns = dynamicDataDetailReq.groupByColumns();
        String limit = dynamicDataDetailReq.limit();

        Map<String, Object> params = new HashMap<>();
        params.put("tableName", tableName);
        params.put("columns", columns);
        params.put("whereClause", whereClause);
        params.put("orderByColumn", orderByColumns);
        params.put("groupByColumn", groupByColumns);
        params.put("limit", limit);
        return dynamicMapper.selectDynamic(params);
    }

    public List<TargetPredictResultData> getPredictResultQuery(DynamicDataReq dynamicDataReq) {

        DataSourceContextHolder.setDataSourceKey(dynamicDataReq.targetDataSource());

        String tableName = dynamicDataReq.tableName();
        String columns = dynamicDataReq.columns();
        String whereClause = dynamicDataReq.whereClause();

        Map<String, Object> params = new HashMap<>();
        params.put("tableName", tableName);
        params.put("columns", columns);
        params.put("whereClause", whereClause);
        params.put("additionalWhereClause", dynamicDataReq.additionalWhereClause());
        params.put("groupByColumn", dynamicDataReq.groupByColumn());
        return dynamicMapper.selectPredictResultData(params);
    }

    public List<DoubleValueResultData> getDoubleValueResultQuery(DynamicDataReq dynamicDataReq) {

        DataSourceContextHolder.setDataSourceKey(dynamicDataReq.targetDataSource());

        String tableName = dynamicDataReq.tableName();
        String columns = dynamicDataReq.columns();
        String whereClause = dynamicDataReq.whereClause();

        Map<String, Object> params = new HashMap<>();
        params.put("tableName", tableName);
        params.put("columns", columns);
        params.put("whereClause", whereClause);
        params.put("additionalWhereClause", dynamicDataReq.additionalWhereClause());
        params.put("groupByColumn", dynamicDataReq.groupByColumn());
        return dynamicMapper.selectDoubleValueResultData(params);
    }

    // @Cacheable(
    //     cacheNames = CacheType.SCHEMA_COLUMN_TYPES_CONSTANT,
    //     key = "#dynamicDataReq.targetDataSource() + ':' + #dynamicDataReq.tableName() + ':' + T(com.sk.common.utils.CombineUtil).getSelectColumnList(#dynamicDataReq.columns()).get(0)",
    //     unless = "#result == null"
    // )
    public SchemaMetadata getMeta(DynamicDataReq dynamicDataReq) {

        DataSourceContextHolder.setDataSourceKey(dynamicDataReq.targetDataSource());

        List<String> select = CombineUtil.getSelectColumnList(dynamicDataReq.columns());

        Map<String, Object> params = new HashMap<>();
        params.put("dbName", dynamicDataReq.targetDataSource());
        params.put("tableName", dynamicDataReq.tableName());
        params.put("colName", select.get(0)); // 날짜 컬럼을 보장함.
        return metadataMapper.selectSchemaMetadata(params);
    }

     /**
     * 예측 데이터 조회
     * @param dynamicDataDetailReq
     * @return
     */
    public List<Map<String, Object>> getDynamicForecast(DynamicDataDetailReq dynamicDataDetailReq) {
        // 데이터 소스 설정
        DataSourceContextHolder.setDataSourceKey(dynamicDataDetailReq.targetDataSource());

        String tableName = dynamicDataDetailReq.tableName();
        String columns = dynamicDataDetailReq.columns();
        String whereClause = dynamicDataDetailReq.whereClause();
        String innerJoin = dynamicDataDetailReq.innerJoin();
        String orderByColumns = dynamicDataDetailReq.orderByColumns();

        Map<String, Object> params = new HashMap<>();
        params.put("tableName", tableName);
        params.put("columns", columns);
        params.put("whereClause", whereClause);
        params.put("innerJoin", innerJoin);
        params.put("orderByColumns", orderByColumns);
        return dynamicMapper.selectDynamicForecastChart(params);
    }
}
