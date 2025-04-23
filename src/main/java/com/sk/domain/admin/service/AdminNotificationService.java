package com.sk.domain.admin.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.model.PaginationExtensionRes;
import com.sk.domain.admin.dto.response.notification.AdminNotificationCountRes;
import com.sk.domain.admin.dto.response.notification.AdminNotificationRes;
import com.sk.domain.shared.repository.master.tlm.EmailAccessLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminNotificationService {

    private final EmailAccessLogRepository emailAccessLogRepository;

    /**
     * 알림 전체 목록을 조회한다.
     * 
     * @param page   페이지 번호
     * @param size   페이지 크기
     * @return 알림 목록
     */
    public PaginationExtensionRes<AdminNotificationRes, AdminNotificationCountRes> getNotifications(int page, int size, Boolean isSendSuccess, String startDate, String endDate) {
        Page<AdminNotificationRes> notifications = emailAccessLogRepository.findNotificationsByConditions(page, size, isSendSuccess, startDate, endDate);
        AdminNotificationCountRes notificationCounts = emailAccessLogRepository.findNotificationCounts(startDate, endDate);

        return PaginationExtensionRes.toRes(notifications, notificationCounts);
    }

}
