package com.sk.domain.mi.service.chart.dto;

import com.sk.domain.shared.entity.master.tmd.UnitConvert;
import com.sk.domain.shared.enums.chart.ChartType;
import com.sk.domain.shared.enums.chart.Interval;
import com.sk.domain.shared.enums.chart.Range;
import lombok.Builder;
import lombok.With;
import org.apache.commons.lang3.tuple.Pair;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * 차트 설정 컨텍스트 레코드.
 * 차트 생성에 필요한 설정 정보를 담고 있습니다.
 */
@With
@Builder
public record ChartSettingsContext(

    /**
     * 인터벌
     */
    Interval finalizedInterval,
    /**
     * 범위
     */
    Range finalizedRange,
    /**
     * 커스텀 범위 시작일
     */
    LocalDate finalizedStartDate,
    /**
     * 커스텀 범위 종료일
     */
    LocalDate finalizedEndDate,
    /**
     * 커스텀 범위 시작일 및 종료일 쌍 객체
     */
    Pair<String, String> finalizedRangeBetween,
    /**
     * 단위변환코드
     */
    UnitCd finalizedUnitCd,
    /**
     * 단위변환코드 문자열
     */
    String finalizedUnitCdStr,
    /**
     * 단위변환을 위한 객체
     */
    UnitConvert finalizedUnitConvert,
    /**
     * 변경가능한 단위변환코드 목록
     */
    Set<UnitCd> finalizedAllowedUnitCds,
    /**
     * 차트타입
     */
    ChartType finalizedChartType,
    /**
     * 보조지표 리스트
     */
    List<String> finalizedIndicators,
    /**
     * 비교군 리스트
     */
    List<Integer> finalizedCompIds

) {
}
