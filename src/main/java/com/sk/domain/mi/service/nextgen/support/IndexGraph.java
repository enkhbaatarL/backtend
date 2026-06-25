package com.sk.domain.mi.service.nextgen.support;

import com.sk.domain.mi.dto.response.nextgen.ChartWindow;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupGraphRes;

/**
 * 인덱스별 그래프 + 클리핑에 쓴 차트 구간
 *
 * @param graph  차트 구간으로 클리핑된 시나리오 그룹 그래프
 * @param window 클리핑에 쓴 차트 표시 구간
 */
public record IndexGraph(
    ScenarioGroupGraphRes graph,
    ChartWindow window
) {
}
