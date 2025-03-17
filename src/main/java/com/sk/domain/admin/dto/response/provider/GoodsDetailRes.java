package com.sk.domain.admin.dto.response.provider;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

public record GoodsDetailRes(

    @Schema(description = "상품번호", example = "5000") //
    Integer goodsNo,

    @Schema(description = "상품명", example = "제조사별 DRAM 생산량") //
    String goodsNm,

    @Schema(description = "상품코드", example = "DRAM") //
    String goodsCd,

    @Schema(description = "상품설명", example = "상품 설명") //
    String goodsExpn,

    @Schema(description = "사용여부", example = "Y") //
    Boolean isUsed,

    @Schema(description = "생성사용자로그인ID", example = "sk01234") //
    String createUserId,

    @Schema(description = "생성일시", example = "2025-02-12 15:00:00") //
    LocalDateTime createDt,

    @Schema(description = "생성사이름", example = "홍길동") //
    String createUserName,

    @Schema(description = "수정사용자로그인ID", example = "sk01234") //
    String modifyUserId,

    @Schema(description = "수정일시", example = "2025-02-12 15:00:00") //
    LocalDateTime modifyDt,

    @Schema(description = "수정자이름", example = "홍길동") //
    String modifyUserName) {
}
