package com.sk.domain.shared.repository.master.tlm.custom;

import org.springframework.data.domain.Page;

import com.sk.domain.admin.dto.response.notification.NotificationRes;

public interface CustomEmailAccessLogRepository {

    Page<NotificationRes> findNotificationsByConditions(int page, int size, String search);
    
}
