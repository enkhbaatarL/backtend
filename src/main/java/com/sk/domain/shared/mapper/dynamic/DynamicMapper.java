package com.sk.domain.shared.mapper.dynamic;

import java.util.List;
import java.util.Map;

import com.sk.domain.shared.mapper.dto.DoubleValueResultData;
import com.sk.domain.shared.mapper.dto.TargetPredictResultData;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DynamicMapper {

    List<Map<String, Object>> selectDynamic(Map<String, Object> params);

    /**
     * '비즈타겟예측결과데이터기본' 테이블 고정 조회
     */
    List<TargetPredictResultData> selectPredictResultData(Map<String, Object> params);

    List<DoubleValueResultData> selectDoubleValueResultData(Map<String, Object> params);

    void saveDynamic(Map<String, String> params);

    void saveAllDynamic(Map<String, Object> param);

    List<Map<String, Object>> selectDynamicForecastChart(Map<String, Object> params);
}
