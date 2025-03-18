package com.sk.domain.admin.dto.response.company;

import lombok.Builder;

public record AuthGoodsMapRes(

    Integer userNo,

    String userNm,

    String departmentName,

    boolean isViewable,

    boolean isDownloadable

) {

    @Builder
    public AuthGoodsMapRes(Integer userNo, String userNm, String departmentName, boolean isViewable, boolean isDownloadable) {
        this.userNo = userNo;
        this.userNm = userNm;
        this.departmentName = departmentName;
        this.isViewable = isViewable;
        this.isDownloadable = isDownloadable;
    }

}
