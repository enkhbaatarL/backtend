package com.sk.domain.admin.dto.response.company;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

public record AuthGoodsMapCntRes(

    @Schema(description = "회원 수")
    Integer numberOfUsers,

    @Schema(description = "조회 권한을 가진 회원 수")
    Long numberOfViewable,

    @Schema(description = "다운로드 권한을 가진 회원 수")
    Long numberOfDownloadable

) {

    @Builder
    public AuthGoodsMapCntRes(Integer numberOfUsers, Long numberOfViewable, Long numberOfDownloadable) {
        this.numberOfUsers = numberOfUsers;
        this.numberOfViewable = numberOfViewable;
        this.numberOfDownloadable = numberOfDownloadable;
    }

    public static AuthGoodsMapCntRes of(List<AuthGoodsMapRes> content) {
        return AuthGoodsMapCntRes.builder()
            .numberOfUsers(content.size())
            .numberOfViewable(content.stream().filter(AuthGoodsMapRes::isViewable).count())
            .numberOfDownloadable(content.stream().filter(AuthGoodsMapRes::isDownloadable).count())
            .build();
    }
}
