package com.sk.domain.shared.mapper.dynamic;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.sk.domain.mi.dto.response.dashboard.charts.DashboardChartFactorMap;

@Mapper
public interface DashboardMapper {
    List<String> chartFactorCategory(String queryWhere, String predictDate);

    List<DashboardChartFactorMap> chartFactors(String queryWhere, String explanatoryLevelNm, String predictDate, String direction, int limit);
}
