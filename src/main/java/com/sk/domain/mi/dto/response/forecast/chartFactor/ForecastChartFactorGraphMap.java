package com.sk.domain.mi.dto.response.forecast.chartFactor;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ForecastChartFactorGraphMap {
    /**
     * 시장지수데이터번호
     */
    String marketIndexDataNo;

    /**
     * RIC코드
     */
    String ricCd;

    /**
     * MNEMONIC코드
     */
    String mnemonicCd;

    /**
     * INSTRUMENT수집테이블명
     */
    String instrumentCollectionTableNm;

    /**
     * INSTRUMENT데이터구분코드
     */
    String instrumentDataSeparateCd;

    /**
     * 상품데이타소스번호
     */
    Integer goodsDataSourceNo;
}
