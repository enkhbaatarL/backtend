package com.sk.domain.mi.dto.response.bbs;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

// @formatter:off
public record MiNoticeDetailRes(
    @Schema(description = "게시물번호", example = "1") 
    Integer bulletinNo,
    
    @Schema(description = "카테고리명", example = "") 
    String bulletinCategoryName,

    @Schema(description = "제목", example = "관리자에서 등록 시 입력한 제목") 
    String bulletinTitleNm,

    @Schema(description = "내용", example = "관리제에서 등록 시 입력한 내용") 
    String bulletinCntt,
    
    @Schema(description = "첨부파일 URL", example = "게시글 등록 시 이미지 첨부된 경우 노출") 
    String fileUrl) {

        @Builder
        public MiNoticeDetailRes(Integer bulletinNo, String bulletinCategoryName, String bulletinTitleNm, String bulletinCntt,String fileUrl) {
            this.bulletinNo = bulletinNo;
            this.bulletinCategoryName = bulletinCategoryName;
            this.bulletinTitleNm = bulletinTitleNm;
            this.bulletinCntt = bulletinCntt;
            this.fileUrl = fileUrl;
        }

        public static MiNoticeDetailRes of(int bulletinNo, String bulletinCategoryName, String bulletinTitleNm, String bulletinCntt, String fileUrl) {
            return MiNoticeDetailRes.builder()
                .bulletinNo(bulletinNo)
                .bulletinCategoryName(bulletinCategoryName)
                .bulletinTitleNm(bulletinTitleNm)
                .bulletinCntt(bulletinCntt)
                .fileUrl(fileUrl)
                .build();
        }
}
// @formatter:on