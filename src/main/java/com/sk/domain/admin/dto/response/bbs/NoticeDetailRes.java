package com.sk.domain.admin.dto.response.bbs;

import java.util.List;

import com.sk.domain.shared.entity.master.tcm.BulletinBoard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

// @formatter:off
public record NoticeDetailRes(
        @Schema(description = "고정여부", example = "true") 
        Boolean isTopFixed,

        @Schema(description = "팝업사용여부", example = "true") 
        Boolean isPopupUse,

        @Schema(description = "팝업게시시작일자", example = "20250101") 
        String popupPostBeginDt,

        @Schema(description = "팝업게시종료일자", example = "20251231") 
        String popupPostEndDt,

        @Schema(description = "카테고리", example = "CAT1") 
        String bulletinCategorySeparateCd,

        @Schema(description = "게시대상(전체:true, 개별:false)", example = "true") 
        Boolean isWholePost,

        @Schema(description = "고객사 정보 배열", example = "") 
        List<BulletinBoardCompanyDto> targetList,

        @Schema(description = "게시글유효시작일자", example = "20250101") 
        String bulletinValidBeginDt,

        @Schema(description = "게시글유효종료일자", example = "20251231") 
        String bulletinValidEndDt,

        @Schema(description = "제목", example = "게시물 제목") 
        String bulletinTitleNm,

        @Schema(description = "내용", example = "게시물 내용") 
        String bulletinCntt,

        @Schema(description = "파일정보", example = "") 
        List<BulletinBoardFileDto> fileList) {

    @Builder
    public NoticeDetailRes(Boolean isTopFixed, Boolean isPopupUse, String popupPostBeginDt, String popupPostEndDt,
            String bulletinCategorySeparateCd, Boolean isWholePost, List<BulletinBoardCompanyDto> targetList,
            String bulletinValidBeginDt, String bulletinValidEndDt, String bulletinTitleNm,
            String bulletinCntt, List<BulletinBoardFileDto> fileList) {
        this.isTopFixed = isTopFixed;
        this.isPopupUse = isPopupUse;
        this.popupPostBeginDt = popupPostBeginDt;
        this.popupPostEndDt = popupPostEndDt;
        this.bulletinCategorySeparateCd = bulletinCategorySeparateCd;
        this.isWholePost = isWholePost;
        this.targetList = targetList;
        this.bulletinValidBeginDt = bulletinValidBeginDt;
        this.bulletinValidEndDt = bulletinValidEndDt;
        this.bulletinTitleNm = bulletinTitleNm;
        this.bulletinCntt = bulletinCntt;
        this.fileList = fileList;
    }

    public static NoticeDetailRes of(BulletinBoard bulletinBoard, List<BulletinBoardCompanyDto> targetList, List<BulletinBoardFileDto> fileList) {

        return NoticeDetailRes.builder()
                .isTopFixed(bulletinBoard.getIsTopFixed())
                .isPopupUse(bulletinBoard.getIsPopupUse())
                .popupPostBeginDt(bulletinBoard.getPopupPostBeginDt())
                .popupPostEndDt(bulletinBoard.getPopupPostEndDt())
                .bulletinCategorySeparateCd(bulletinBoard.getBulletinCategorySeparateCd())
                .isWholePost(bulletinBoard.getIsWholePost())
                .targetList(targetList)
                .bulletinValidBeginDt(bulletinBoard.getBulletinValidBeginDt())
                .bulletinValidEndDt(bulletinBoard.getBulletinValidEndDt())
                .bulletinTitleNm(bulletinBoard.getBulletinTitleNm())
                .bulletinCntt(bulletinBoard.getBulletinCntt())
                .fileList(fileList)
                .build();
    }
}
// @formatter:on