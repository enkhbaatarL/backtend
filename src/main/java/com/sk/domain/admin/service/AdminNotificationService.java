package com.sk.domain.admin.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.request.NotificationReq;
import com.sk.domain.admin.dto.response.NotificationRes;
import com.sk.domain.shared.entity.master.tmd.Goods;
import com.sk.domain.shared.entity.master.tmd.GoodsCriticalValueSetting;
import com.sk.domain.shared.repository.master.tlm.EmailAccessLogRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsCriticalValueSettingRepository;
import com.sk.domain.shared.service.MailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminNotificationService {

    private final EmailAccessLogRepository emailAccessLogRepository;
    private final GoodsCriticalValueSettingRepository goodsCriticalValueSettingRepository;
    private final MailService mailService;

    /**
     * 알림 목록을 조회한다.
     * 
     * @param userNo 사용자 번호
     * @param page   페이지 번호
     * @param size   페이지 크기
     * @return 알림 목록
     */
    public PaginationRes<NotificationRes> getNotifications(int page, int size) {
        return PaginationRes.toRes(emailAccessLogRepository.findNotificationsByConditions(page, size));
    }

    /**
     * 임계치를 확인하고 알림을 발송한다.
     * 
     * @param notificationReq 알림 요청
     * @return 알림 발송 결과
     */
    public Boolean createNotification() {
        List<GoodsCriticalValueSetting> goodsCriticalValueSettings = goodsCriticalValueSettingRepository
                .findAllByIsDeletedFalse();

        for (GoodsCriticalValueSetting goodsCriticalValueSetting : goodsCriticalValueSettings) {
            //TODO: 호영: 임계치에 따른 이메일 발송 로직 추가, 성공한 메일, 실패한 메일 나눠서 Response 작성

            // Integer goodsDataSourceNo = goodsCriticalValueSetting.getGoodsDataSource().getGoodsDataSourceNo();
            // String goodsDataSourceNm = goodsCriticalValueSetting.getGoodsDataSource().getGoodsDataSourceNm();
            // Goods goods = goodsCriticalValueSetting.getGoodsDataSource().getGoods();
            // String targetDbNm = goodsCriticalValueSetting.getGoodsDataSource().getTargetDbNm();
            // String targetTableNm = goodsCriticalValueSetting.getGoodsDataSource().getTargetTableNm();
            // String queryConditionContent = goodsCriticalValueSetting.getGoodsDataSource().getQueryConditionContent();
            

            // mailService.sendMail(goodsCriticalValueSetting.getUser().getEmail(), "알림", "알림");
        }

        return true;
    }
}
