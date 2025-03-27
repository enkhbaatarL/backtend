package com.sk.domain.admin.dto.request.provider;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

import org.hibernate.annotations.Comment;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record GoodsDataCreateReq(
    @NotBlank
    @Size(min = 1, max = 100)
    @Schema(description = "데이터명", example = "")
    String goodsDataNm,

    @NotBlank
    @Schema(description = "Summary", example = "")
    String goodsDataExpn,

    @NotNull
    @Schema(description = "데이타 Category Code Id", example = "")
    List<String> categoryCodeIdList,

    @Schema(description = "정형 데이타 리스트", example = "")
    List<StructuredDataDto> structuredDataList,

    @Schema(description = "비정형 데이타 리스트", example = "")
    List<UnstructuredDataDto> StructuredDataList) {

}
// @formatter:on