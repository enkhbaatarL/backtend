package com.sk.domain.admin.dto.response.bbs;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sk.domain.shared.entity.master.tcm.BulletinBoard;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record NoticeDetailRes(
        @Schema(description = "고정여부", example = "true")
        Boolean isTopFixed,

        @Schema(description = "팝업사용여부", example = "true")
        Boolean isPopupUse,

        @NotNull
        @JsonFormat(pattern = "yyyyMMdd")
        @DateTimeFormat(pattern = "yyyyMMdd")
        @Schema(description = "팝업게시시작일자", example = "20250101")
        LocalDate popupPostBeginDt,

        @NotNull
        @JsonFormat(pattern = "yyyyMMdd")
        @DateTimeFormat(pattern = "yyyyMMdd")
        @Schema(description = "팝업게시종료일자", example = "20251231")
        LocalDate popupPostEndDt,

        @NotBlank
        @Schema(description = "카테고리", example = "CAT1")
        String bulletinCategorySeparateCode,

        @NotNull
        @Schema(description = "게시대상(전체:true, 개별:false)", example = "true")
        Boolean isWholePost,

        @Schema(description = "고객서 ID 배열- 게시대상에 따라 null 또는 고객사 ID 배열", example = "[1,2,3]")
        List<Integer> target,

        @NotNull
        @JsonFormat(pattern = "yyyyMMdd")
        @DateTimeFormat(pattern = "yyyyMMdd")
        @Schema(description = "게시글유효시작일자", example = "20250101")
        LocalDate bulletinValidBeginDt,

        @NotNull
        @JsonFormat(pattern = "yyyyMMdd")
        @DateTimeFormat(pattern = "yyyyMMdd")
        @Schema(description = "게시글유효종료일자", example = "20251231")
        LocalDate bulletinValidEndDt,

        @NotBlank
        @Size(min = 1, max = 50)
        @Schema(description = "제목", example = "게시물 제목")
        String bulletinTitleName,

        @NotBlank
        @Schema(description = "내용", example = "게시물 내용")
        String bulletinContent,

        @Schema(description = "파일번호", example = "1")
        Long fileNo,

        @Schema(description = "파일번호", example = "1")
        Long fileNm,

        @Schema(description = "파일번호", example = "1")
        Long fileSize) {

    public static NoticeDetailRes of(BulletinBoard bulletinBoard, List<Integer> target, UploadedFile uploadedFile) {

        return NoticeDetailRes.builder()
                .build();
    }
}
