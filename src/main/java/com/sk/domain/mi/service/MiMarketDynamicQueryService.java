package com.sk.domain.mi.service;

import com.sk.common.utils.CombineUtil;
import com.sk.common.utils.DateBindingUtil;
import com.sk.common.utils.DateUtil;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataReq;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.mi.dto.response.marketData.charts.graph.MarketDataChartRawData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.mapper.dto.SchemaMetadata;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MiMarketDynamicQueryService {
    private final MybatisDynamicQueryService mybatisDynamicQueryService;

    /**
     * 날짜 범위를 기준으로 데이터베이스에서 데이터 행을 조회한다.
     * 데이터소스 상세 정보와 날짜 컬럼명, 시작일을 기반으로 데이터를 필터링하여 조회한다.
     * 조건에 맞는 데이터가 없는 경우 빈 리스트를 반환한다.
     *
     * @param goodsDataSourceDetail 데이터소스 상세 정보
     * @param dateColumnName        필터링할 날짜 컬럼명
     * @param startDate             데이터 범위 시작일 (포함, yyyy-MM-dd 형식)
     * @return 조건에 맞는 데이터 행 목록
     */
    @Transactional
    public List<Map<String, Object>> getMarketDataRows(GoodsDataSourceDetail goodsDataSourceDetail, String dateColumnName, LocalDate startDate,
        LocalDate endDate) {
        if (goodsDataSourceDetail == null || dateColumnName == null || startDate == null) {
            return Collections.emptyList();
        }

        // 날짜 컬럼 스키마 조회
        SchemaMetadata meta = mybatisDynamicQueryService.getMeta(DynamicDataReq.ofMetaQuery(goodsDataSourceDetail));
        Pair<String, String> dateRange = DateBindingUtil.range(meta, startDate, endDate);

        String whereClause = CombineUtil.createBetweenClause(dateColumnName, dateRange.getLeft(), dateRange.getRight());

        return mybatisDynamicQueryService.getDynamicData(DynamicDataReq.of(goodsDataSourceDetail, whereClause, dateColumnName));
    }

    /**
     * DTO 생성
     *
     * @param rawData 원시 데이터
     * @param dateColumnName 날짜 컬럼명
     * @param defaultDataColumnName 기본 데이터 컬럼명
     * @param startDate 월별 평균 계산 기준 날짜
     * @return DTO 생성
     */
    public static List<MarketDataChartRawData> convertToDashBoardData(List<Map<String, Object>> rawData,
        String dateColumnName,
        String defaultDataColumnName,
        LocalDate startDate) {
        if (rawData == null || rawData.isEmpty()) {
            return Collections.emptyList();
        }

        int size = rawData.size();
        int limit = 60;

        return rawData.stream().skip(size > limit ? size - limit : 0)
            .map(r->{
                String date = DateUtil.toYmdHyphen(DateUtil.parseDate(r.get(dateColumnName)));
                Object value = r.get(defaultDataColumnName);

                return MarketDataChartRawData.builder()
                    .date(date)
                    .value(value == null ? null : Double.parseDouble(value.toString()))
                    .build()
                    ;
            })
            .toList();
    }
}
