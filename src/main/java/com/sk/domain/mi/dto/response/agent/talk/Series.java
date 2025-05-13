package com.sk.domain.mi.dto.response.agent.talk;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record Series(

    String seriesNm,
    List<BigDecimal> values

) {
}
