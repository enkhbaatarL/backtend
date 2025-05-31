package com.sk.domain.mi.dto.response.dashboard;

import com.sk.domain.shared.enums.PriceType;

import java.math.BigDecimal;
import java.util.List;

// 권한여부
//
public record HistoryDataRes(

    String date,
    PriceType priceType,
    String priceTypeNm,
    List<Item> forecasts

) {

    public record Item(

        BigDecimal value,
        boolean isMaximum,
        boolean isMinimum,
        Double changeRate

    ) {

    }

}
