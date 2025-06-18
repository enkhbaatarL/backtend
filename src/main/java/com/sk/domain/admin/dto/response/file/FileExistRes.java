package com.sk.domain.admin.dto.response.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

// @formatter:off
public record FileExistRes(
        @Schema(description = "상품데이터번호", example = "")
        Integer goodsDataNo,

        @Schema(description = "파일번호", example = "")
        Long fileNo
        ) {

    @Builder
    public FileExistRes(Integer goodsDataNo, Long fileNo) {
        this.goodsDataNo = goodsDataNo;
        this.fileNo = fileNo;
    }

}
// @formatter:on