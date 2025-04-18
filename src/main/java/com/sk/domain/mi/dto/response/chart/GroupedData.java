package com.sk.domain.mi.dto.response.chart;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GroupedData(

    LocalDate startDate,
    LocalDate endDate,
    long count,
    BigDecimal averageValue,
    BigDecimal minValue,
    BigDecimal maxValue,
    BigDecimal initialValue,
    BigDecimal finalValue

) {
}
