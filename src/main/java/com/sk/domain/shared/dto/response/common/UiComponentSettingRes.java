package com.sk.domain.shared.dto.response.common;

import com.sk.domain.shared.entity.master.tcm.UiComponentSetting;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "UI 컴포넌트 설정 조회 응답")
public record UiComponentSettingRes(
    @Schema(description = "고객회사번호") Integer companyNo,
    @Schema(description = "화면코드") String screenCode,
    @Schema(description = "컴포넌트타입코드") String componentTypeCode,
    @Schema(description = "컴포넌트코드") String componentCode,
    @Schema(description = "항목키") String itemKey,
    @Schema(description = "메시지코드") String messageCode,
    @Schema(description = "라벨텍스트") String labelText,
    @Schema(description = "표시여부") Boolean isVisible,
    @Schema(description = "정렬순서") Integer orderNo,
    @Schema(description = "설정값") String settingValue
) {
    public static UiComponentSettingRes from(UiComponentSetting setting) {
        return new UiComponentSettingRes(
            setting.getCustomerCompany().getCustomerCompanyNo(),
            setting.getScreenCode(),
            setting.getComponentTypeCode(),
            setting.getComponentCode(),
            setting.getItemKey(),
            setting.getMessageCode(),
            setting.getLabelText(),
            setting.getIsVisible(),
            setting.getOrderNo(),
            setting.getSettingValue()
        );
    }
}
