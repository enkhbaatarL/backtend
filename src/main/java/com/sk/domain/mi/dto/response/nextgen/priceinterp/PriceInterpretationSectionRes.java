package com.sk.domain.mi.dto.response.nextgen.priceinterp;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 시나리오별 가격전망 해석 section (그룹 상세 응답에 embed).
 * generatable은 '가격전망 해석' 버튼 활성 여부
 */
public record PriceInterpretationSectionRes(

    @Schema(description = "'가격전망 해석' 생성 버튼 활성 여부", example = "true")
    boolean generatable,

    @Schema(description = "해석 보유 시나리오 목록")
    List<PriceInterpretationItemRes> interpretations

) {

    public static PriceInterpretationSectionRes of(boolean generatable, List<PriceInterpretationItemRes> interpretations) {
        return new PriceInterpretationSectionRes(generatable, interpretations);
    }

}
