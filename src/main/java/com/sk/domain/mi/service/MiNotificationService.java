package com.sk.domain.mi.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.response.user.UserRes;
import com.sk.domain.mi.dto.request.notification.NotificationSettingReq;
import com.sk.domain.mi.dto.response.notification.MiNotificationRes;
import com.sk.domain.mi.dto.response.notification.MiNotificationSettingGoodsDataSourceRes;
import com.sk.domain.mi.dto.response.notification.MiNotificationSettingRes;
import com.sk.domain.mi.dto.response.notification.MiUserNotificationSettingRes;
import com.sk.domain.shared.entity.compositeKey.tmd.UserHistoryId;
import com.sk.domain.shared.entity.master.tlm.EmailAccessLog;
import com.sk.domain.shared.entity.master.tmd.GoodsCriticalValueSetting;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.entity.master.tmd.UserHistory;
import com.sk.domain.shared.repository.master.tlm.EmailAccessLogRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsCriticalValueSettingRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataSourceDetailRepository;
import com.sk.domain.shared.repository.master.tmd.UserHistoryRepository;
import com.sk.domain.shared.repository.master.tmd.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiNotificationService {

    private final GoodsCriticalValueSettingRepository goodsCriticalValueSettingRepository;
    private final GoodsDataSourceDetailRepository goodsDataSourceDetailRepository;
    private final EmailAccessLogRepository emailAccessLogRepository;
    private final UserRepository userRepository;
    private final UserHistoryRepository userHistoryRepository;



    /**
     * 사용자 알림 설정 조회
     * 
     * @param user
     * @return
     */
    public MiUserNotificationSettingRes getUserNotificationSetting(User user) {
        return MiUserNotificationSettingRes.of(user);
    }

    /**
     * 사용자 알림 설정 업데이트
     * 
     * @param isUserNotificationSetting
     * @param user
     * @return
     */
    @Transactional
    public UserRes updateUserNotificationSetting(NotificationSettingReq.UpdateUserSetting dto, User user) {
        UserHistory userHistory = UserHistory.builder()
                .id(new UserHistoryId(user.getUserNo(), LocalDateTime.now()))
                .isOtherCompanyServiceAccessPossible(user.getIsOtherServiceAccessPossible())
                .user(user)
                .build();
        userHistoryRepository.save(userHistory);

        user.updateNotificationSetting(dto.isUserNotificationSetting());
        return UserRes.ofSimple(userRepository.save(user));
    }

    /**
     * 알림 설정 등록
     * 
     * @param notification
     * @param goodsDataSourceNo
     * @param user
     * @return
     */
    @Transactional
    public MiNotificationSettingRes createNotificationSetting(Integer goodsDataSourceNo, NotificationSettingReq.Insert notifications, User user) {

        GoodsDataSourceDetail goodsDataSourceDetail = goodsDataSourceDetailRepository
                .findByGoodsDataSourceNoAndIsDeletedFalse(goodsDataSourceNo)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                        String.format("GoodsDataSourceDetail not found: goodsDataSourceNo=%d", goodsDataSourceNo)));

        GoodsCriticalValueSetting goodsCriticalValueSetting = GoodsCriticalValueSetting.of(notifications, goodsDataSourceDetail, user);

        GoodsCriticalValueSetting savedGoodsCriticalValueSetting = goodsCriticalValueSettingRepository
                .save(goodsCriticalValueSetting);

        return MiNotificationSettingRes.of(savedGoodsCriticalValueSetting, goodsDataSourceDetail);
    }

    /**
     * 알림 설정한 정형 데이터 목록 조회
     * 
     * @param user
     * @param page
     * @param size
     * @return
     */
    public PaginationRes<MiNotificationSettingGoodsDataSourceRes> getNotificationSettingsGoodsDataSource(int page, int size, String search,
            User user) {
        return PaginationRes.toRes(goodsCriticalValueSettingRepository.findNotificationSettingGoodsDataSourceByConditions(page, size, search, user));
    }

    /**
     * 상품 데이터의 알림설정 리스트를 조회
     * 
     * @param goodsDataSourceNo
     * @param user
     * @return
     */
    public PaginationRes<MiNotificationSettingRes> getNotificationSettings(int page, int size, Integer goodsDataSourceNo, User user) {
        GoodsDataSourceDetail goodsDataSourceDetail = goodsDataSourceDetailRepository
                .findByGoodsDataSourceNoAndIsDeletedFalse(goodsDataSourceNo)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                        String.format("GoodsDataSourceDetail not found: goodsDataSourceNo=%d", goodsDataSourceNo)));

        return PaginationRes.toRes(goodsCriticalValueSettingRepository
                .findNotificationSettingsByConditions(goodsDataSourceDetail, user, page, size));
    }

    /**
     * 상품 데이터의 알림설정 상세를 조회
     * 
     * @param goodsDataSourceNo
     * @param notificationSettingNo
     * @param user
     * @return
     */
    public MiNotificationSettingRes getNotificationSetting(Integer goodsDataSourceNo, Integer notificationSettingNo, User user) {
        GoodsDataSourceDetail goodsDataSourceDetail = goodsDataSourceDetailRepository
                .findByGoodsDataSourceNoAndIsDeletedFalse(goodsDataSourceNo)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                        String.format("GoodsCriticalValueSetting not found: notificationSettingNo=%d",
                                notificationSettingNo)));

        GoodsCriticalValueSetting goodsCriticalValueSetting = goodsCriticalValueSettingRepository
                .findByUserGoodsNotificationSettingNoAndUserAndIsDeletedFalse(notificationSettingNo, user)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                        String.format("GoodsCriticalValueSetting not found: notificationSettingNo=%d",
                                notificationSettingNo)));

        return MiNotificationSettingRes.ofDetail(goodsCriticalValueSetting, goodsDataSourceDetail, goodsDataSourceDetail.getGoodsData(), goodsDataSourceDetail.getGoodsData().getGoods());
    }

    /**
     * 알림설정 삭제
     * 
     * @param goodsDataSourceNo
     * @param user
     * @return
     */
    @Transactional
    public MiNotificationSettingRes deleteNotificationSetting(Integer goodsDataSourceNo, Integer notificationSettingNo,
            User user) {
        GoodsDataSourceDetail goodsDataSourceDetail = goodsDataSourceDetailRepository
                .findByGoodsDataSourceNoAndIsDeletedFalse(goodsDataSourceNo)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                        String.format("GoodsCriticalValueSetting not found: goodsDataSourceNo=%d", goodsDataSourceNo)));

        GoodsCriticalValueSetting goodsCriticalValueSetting = goodsCriticalValueSettingRepository
                .findByUserGoodsNotificationSettingNoAndUserAndIsDeletedFalse(notificationSettingNo, user)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                        String.format("GoodsCriticalValueSetting not found: notificationSettingNo=%d",
                                notificationSettingNo)));

        goodsCriticalValueSetting.softDelete();

        return MiNotificationSettingRes.of(goodsCriticalValueSetting, goodsDataSourceDetail);
    }

    /**
     * 알림 발송 내역 리스트 조회
     * 
     * @param user
     * @param page
     * @param size
     * @return
     */
    public PaginationRes<MiNotificationRes> getNotifications(int page, int size, User user) {
        Page<MiNotificationRes> emailAccessLogs = emailAccessLogRepository.findNotificationsWithinSevenDays(page, size, user);
        return PaginationRes.toRes(emailAccessLogs);
    }

    /**
     * 알림 발송 내역 상세보기
     * 
     * @param emailSendSn
     * @param user
     * @return
     */
    public MiNotificationRes getNotification(Integer emailSendSn, User user) {
        EmailAccessLog emailAccessLog = emailAccessLogRepository
                .findByEmailSendSnAndEmailReceiveUserLoginIdAndIsDeletedFalse(emailSendSn, user.getUserLoginId())
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                        String.format("EmailAccessLog not found: emailSendSn=%d", emailSendSn)));
        return MiNotificationRes.of(emailAccessLog);
    }

    /**
     * 알림 내역 조회 처리
     * 
     * 
     * @param emailSendSn
     * @param user
     * @return
     */
    @Transactional
    public MiNotificationRes inquiryNotification(Integer emailSendSn, User user) {
        EmailAccessLog emailAccessLog = emailAccessLogRepository
                .findByEmailSendSnAndEmailReceiveUserLoginIdAndIsDeletedFalse(emailSendSn, user.getUserLoginId())
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                        String.format("EmailAccessLog not found: emailSendSn=%d", emailSendSn)));

        emailAccessLog.updateInquiry();

        return MiNotificationRes.of(emailAccessLog);
    }
}
