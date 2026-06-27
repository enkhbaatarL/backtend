package com.sk.domain.admin.dto.response.provider;

import com.sk.common.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

// @formatter:off
public record GoodsDataRes(
        @Schema(description = "고객회사명", example = "")
        String customerCompanyNm,

        @Schema(description = "상품데이터번호", example = "")
        Integer goodsDataNo,

        @Schema(description = "상품데이터명", example = "")
        String goodsDataNm,

        @Schema(description = "상품데이터설명", example = "")
        String goodsDataExpn,

        @Schema(description = "상품데이터 발행날짜", example = "2025-12-31")
        String publishedDate,

        @Schema(description = "상품데이터 발행일시", example = "2025-12-31 15:30:45")
        String publishedDateTime) {

    public GoodsDataRes(String customerCompanyNm, Integer goodsDataNo, String goodsDataNm, String goodsDataExpn, LocalDate publishedDate, LocalDateTime publishedDateTmp) {
        this(customerCompanyNm, goodsDataNo, goodsDataNm, goodsDataExpn,
                publishedDate != null ? DateUtil.toYmdHyphen(publishedDate) : null,
                publishedDateTmp != null ? DateUtil.toYyyyMmDdHms(publishedDateTmp) : null);
    }
}
// @formatter:on
