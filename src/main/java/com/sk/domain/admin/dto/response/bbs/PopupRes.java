package com.sk.domain.admin.dto.response.bbs;

import java.util.List;

import com.sk.domain.shared.entity.master.tcm.BulletinBoard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

// @formatter:off
public record PopupRes(
        @Schema(description = "게시물번호", example = "1") 
        Integer bulletinNo,

        @Schema(description = "제목", example = "제목") 
        String bulletinTitleName,

        @Schema(description = "내용", example = "게시물 내용") 
        String bulletinContent,

        @Schema(description = "파일정보", example = "") 
        List<BulletinBoardFileDto> fileList) {

    @Builder
    public PopupRes(Integer bulletinNo, String bulletinTitleName, String bulletinContent, List<BulletinBoardFileDto> fileList) {
        this.bulletinNo = bulletinNo;
        this.bulletinTitleName = bulletinTitleName;
        this.bulletinContent = bulletinContent;
        this.fileList = fileList;
    }

    public static PopupRes of(BulletinBoard bulletinBoard, List<BulletinBoardFileDto> fileList) {

        return PopupRes.builder()
                .bulletinNo(bulletinBoard.getBulletinNo())
                .bulletinTitleName(bulletinBoard.getBulletinTitleNm())
                .bulletinContent(bulletinBoard.getBulletinCntt())
                .fileList(fileList)
                .build();
    }
}
// @formatter:on