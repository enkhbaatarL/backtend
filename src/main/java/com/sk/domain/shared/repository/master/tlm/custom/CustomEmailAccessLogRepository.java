package com.sk.domain.shared.repository.master.tlm.custom;

import org.springframework.data.domain.Page;

import com.sk.domain.admin.dto.response.notification.AdminNotificationRes;
import com.sk.domain.mi.dto.response.notification.MiNotificationRes;

public interface CustomEmailAccessLogRepository {

    Page<AdminNotificationRes> findNotificationsByConditions(int page, int size, String search);
    
    Page<MiNotificationRes> findNotificationsWithinSevenDays(int page, int size);

}
