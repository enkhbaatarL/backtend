package com.sk.domain.mi.dto.response.notification;

import com.sk.domain.shared.entity.master.tmd.User;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record MiUserNotificationSettingRes(
        @Schema(description = "사용자 알림 설정 여부", example = "true") Boolean isUserNotificationSetting
) {
    @Builder
    public MiUserNotificationSettingRes(Boolean isUserNotificationSetting) {
        this.isUserNotificationSetting = isUserNotificationSetting;
    }

    public static MiUserNotificationSettingRes of(User user) {
        return MiUserNotificationSettingRes.builder()
                .isUserNotificationSetting(user.getIsUserNotificationSetting())
                .build();
    }
}

