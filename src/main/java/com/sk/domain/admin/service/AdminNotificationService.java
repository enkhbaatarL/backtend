package com.sk.domain.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.model.PaginationRes;
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
    public PaginationRes<AdminNotificationRes> getNotifications(int page, int size, Boolean isSendSuccess, String startDate, String endDate) {
        return PaginationRes.toRes(emailAccessLogRepository.findNotificationsByConditions(page, size, isSendSuccess, startDate, endDate));


        // TODO: LastUpdated, 성공 / 실패 카운트 추가
        // PaginationExtensionRes 이용 예정
    }

}
