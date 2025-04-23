package com.sk.domain.admin.dto.response.company;

import com.sk.common.utils.DateUtil;
import com.sk.common.utils.StringUtil;
import com.sk.domain.shared.entity.master.tmd.User;
import io.micrometer.common.util.StringUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.apache.commons.lang3.tuple.Pair;

public record AuthGoodsMapRes(

    @Schema(description = "사용자 번호", example = "1")
    Integer userNo,

    @Schema(description = "사용자 이름", example = "홍길동")
    String userNm,

    @Schema(description = "직책 명", example = "수석")
    String onDutyNm,

    @Schema(description = "부서 명", example = "디지털 혁신부")
    String departmentNm,

    @Schema(description = "보기권한 여부", example = "true")
    boolean isViewable,

    @Schema(description = "다운로드권한 여부", example = "false")
    boolean isDownloadable,

    @Schema(description = "권한 시작 일자", example = "20230101")
    String usageBeginDate,

    @Schema(description = "권한 종료 일자", example = "20230110")
    String usageEndDate

) {

    @Builder
    public AuthGoodsMapRes(Integer userNo, String userNm, String onDutyNm, String departmentNm, boolean isViewable, boolean isDownloadable, String usageBeginDate, String usageEndDate) {
        this.userNo = userNo;
        this.userNm = userNm;
        this.onDutyNm = onDutyNm;
        this.departmentNm = departmentNm;
        this.isViewable = isViewable;
        this.isDownloadable = isDownloadable;
        this.usageBeginDate = usageBeginDate;
        this.usageEndDate = usageEndDate;
    }

    public static AuthGoodsMapRes of(User user, Boolean[] auth, Pair<String, String> usePeriod) {
        return AuthGoodsMapRes.builder()
            .userNo(user.getUserNo())
            .userNm(user.getUserNm())
            .onDutyNm(user.getOnDutyNm())
            .departmentNm(user.getDepartmentNm())
            .isViewable(auth[0])
            .isDownloadable(auth[1])
            .usageBeginDate(StringUtils.isNotEmpty(usePeriod.getLeft()) ? usePeriod.getLeft() : DateUtil.currentYmd())
            .usageEndDate(StringUtils.isNotEmpty(usePeriod.getRight()) ? usePeriod.getRight() : DateUtil.after10yYmd())
            .build();
    }

}
