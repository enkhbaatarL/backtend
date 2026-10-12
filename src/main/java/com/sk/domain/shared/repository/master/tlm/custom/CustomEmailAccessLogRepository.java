package com.sk.domain.shared.repository.master.tlm.custom;

import java.util.List;
import org.springframework.data.domain.Page;

import com.sk.domain.admin.dto.response.notification.AdminNotificationCountRes;
import com.sk.domain.admin.dto.response.notification.AdminNotificationRes;
import com.sk.domain.mi.dto.response.notification.MiNotificationRes;
import com.sk.domain.shared.entity.master.tmd.User;

public interface CustomEmailAccessLogRepository {

    Page<AdminNotificationRes> findNotificationsByConditions(int page, int size, Boolean isSendSuccess, String startDate, String endDate);

    Page<MiNotificationRes> findNotificationsWithinSevenDays(int page, int size, User user);

    /** 알림벨 상세 조회 — 본인 수신 + 미삭제만 (기존 목록과 같은 프로젝션) */
    List<MiNotificationRes> findNotificationsByEmailSendSns(List<Integer> emailSendSns, String userLoginId);

    AdminNotificationCountRes findNotificationCounts(String startDate, String endDate);

}
