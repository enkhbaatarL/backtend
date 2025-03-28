package com.sk.domain.mi.conversion.composer;

import com.sk.domain.mi.conversion.strategy.ConversionStrategy;
import com.sk.domain.mi.conversion_on_demand.FormulaService;

import java.util.List;

/**
 * 여러 전략을 조합하는 방식을 정의하는 인터페이스
 * 전략 자체와 전략 조합 방식을 분리하기 위한 컴포저 패턴
 */
public interface StrategyComposer {

    /**
     * 여러 전략을 조합하여 단일 전략으로 반환
     *
     * @param strategies 조합할 전략 목록
     * @param targetService 결과 전략이 제공할 서비스
     * @return 조합된 단일 전략
     */
    ConversionStrategy compose(List<ConversionStrategy> strategies, FormulaService targetService);
}
