package com.sk.domain.shared.mapper.dynamic;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;

import com.sk.domain.mi.dto.response.dashboard.chartFactor.DashboardChartFactorExplanatoryMap;
import com.sk.domain.mi.dto.response.dashboard.chartFactor.DashboardChartFactorGraphMap;
import com.sk.domain.mi.dto.response.dashboard.chartFactor.DashboardChartFactorMap;
import com.sk.domain.mi.dto.response.dashboard.summary.DashboardChartSummaryMap;

@Mapper
public interface DashboardMapper {
    /**
     * 차트 주요 요인 분석 카테고리 조회(Drill Down #1)
     * 
     * @param queryWhere 조회 조건
     * @param predictDate 예측 날짜
     * @return List<String>
     */
    List<String> chartFactorCategory(String queryWhere, String predictDate);

    /**
     * 차트 주요 요인 분석 조회(Drill Down #2)
     * 
     * @param queryWhere 조회 조건
     * @param explanatoryLevelNm 주요 요인 레벨명
     * @param predictDate 예측 날짜
     * @param direction 방향
     * @param limit 제한 개수
     * @return List<DashboardChartFactorMap>
     */
    List<DashboardChartFactorMap> chartFactors(String queryWhere, String explanatoryLevelNm, String predictDate, String direction, int limit);

    /**
     * 차트 주요 요인 분석 설명력 조회(Drill Down #2)
     * 
     * @param queryWhere 조회 조건
     * @param explanatoryLevelNm 주요 요인 레벨명
     * @param predictDate 예측 날짜
     * @return DashboardChartFactorExplanatoryMap
     */
    Optional<DashboardChartFactorExplanatoryMap> chartFactorExplanatory(String queryWhere, String explanatoryLevelNm, String predictDate);
    
    /**
     * 차트 주요 요인 분석 영향 데이터 설명력 조회(Drill Down #3)
     * 
     * @param queryWhere 조회 조건
     * @param explanatoryLevelNm 주요 요인 레벨명
     * @param predictDate 예측 날짜
     * @param predictionInputMnemonicCd 예측 입력 MNEMONIC코드
     * @return DashboardChartFactorExplanatoryMap
     */
    Optional<DashboardChartFactorExplanatoryMap> chartFactorGraphExplanatory(String queryWhere, String explanatoryLevelNm, String predictDate, String predictionInputMnemonicCd);

    /**
     * 차트 주요 요인 분석 영향 데이터 테이블 조회(Drill Down #3)
     * 
     * @param marketIndexAnalysisTargetCd 시장지수분석대상코드
     * @param mnemonicCd MNEMONIC코드
     * @return DashboardChartFactorGraphMap
     */
    Optional<DashboardChartFactorGraphMap> chartFactorGraph(String marketIndexAnalysisTargetCd, String mnemonicCd);
    
    /**
     * 차트 요약 조회
     * 
     * @param queryWhere 조회 조건
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return DashboardChartSummaryMap
     */
    Optional<DashboardChartSummaryMap> chartSummary(String queryWhere, String startDate, String endDate);
}
