package com.sk.domain.admin.dto.response.bbs;

public record BulletinBoardFileDto(
        Long fileNo,
        String fileNm,
        Long fileSize,
        String fileUrl) {
}
