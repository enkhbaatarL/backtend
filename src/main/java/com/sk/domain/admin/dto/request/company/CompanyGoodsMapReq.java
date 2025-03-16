package com.sk.domain.admin.dto.request.company;

import com.sk.domain.shared.enums.GoodsAuthManageSupportCd;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CompanyGoodsMapReq(

    @NotEmpty
    @Schema(description = "상품번호", example = "1")
    Integer goodsNo,

    @Schema(description = "사용여부", example = "true")
    boolean isUsed,

    @NotNull
    @Schema(description = "상품권한주체구분코드", example = "INDIVIDUAL", allowableValues = {"INDIVIDUAL", "COMPANY"})
    GoodsAuthManageSupportCd goodsAuthManageCd

) {
}
