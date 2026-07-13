package com.sk.domain.mi.dto.request.nextgen.priceinterp;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 시나리오별 가격전망 해석 수정 요청
 */
public record PriceInterpretationEditReq(

    @NotNull
    @Size(max = 2000, message = "가격전망 해석은 2000자를 넘을 수 없습니다.")
    String content

) {}
