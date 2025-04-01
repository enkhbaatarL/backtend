package com.sk.domain.mi.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.request.notification.NotificationReq;
import com.sk.domain.mi.dto.response.notification.NotificationRes;
import com.sk.domain.shared.entity.master.tmd.GoodsCriticalValueSetting;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.repository.master.tmd.GoodsCriticalValueSettingRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataSourceDetailRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiNotificationService {

    private final GoodsCriticalValueSettingRepository goodsCriticalValueSettingRepository;
    private final GoodsDataSourceDetailRepository goodsDataSourceDetailRepository;

    /**
     * 알림 등록
     * 
     * @param notification
     * @param goodsDataSourceNo
     * @param user
     * @return
     */
    @Transactional
    public List<NotificationRes> createNotification(NotificationReq.Insert notifications, Integer goodsDataSourceNo,
            User user) {

        GoodsDataSourceDetail goodsDataSourceDetail = goodsDataSourceDetailRepository
                .findByGoodsDataSourceNoAndIsDeletedFalse(goodsDataSourceNo)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                        "GoodsCriticalValueSetting not found"));

        List<GoodsCriticalValueSetting> goodsCriticalValueSettings = notifications.settings().stream()
                .map(notification -> GoodsCriticalValueSetting.of(notification, goodsDataSourceDetail, user))
                .collect(Collectors.toList());

        List<GoodsCriticalValueSetting> savedGoodsCriticalValueSettings = goodsCriticalValueSettingRepository
                .saveAll(goodsCriticalValueSettings);

        return savedGoodsCriticalValueSettings.stream()
                .map(NotificationRes::of)
                .collect(Collectors.toList());
    }

    /**
     * 알림 조회
     * 
     * @param goodsDataSourceNo
     * @param user
     * @return
     */
    public List<NotificationRes> getNotification(Integer goodsDataSourceNo, User user) {
        GoodsDataSourceDetail goodsDataSourceDetail = goodsDataSourceDetailRepository
                .findByGoodsDataSourceNoAndIsDeletedFalse(goodsDataSourceNo)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                        "GoodsCriticalValueSetting not found"));

        List<GoodsCriticalValueSetting> goodsCriticalValueSettings = goodsCriticalValueSettingRepository
                .findByGoodsDataSourceDetailAndUserAndIsDeletedFalse(goodsDataSourceDetail, user);

        return goodsCriticalValueSettings.stream()
                .map(NotificationRes::of)
                .collect(Collectors.toList());
    }

    /**
     * 알림 수정 (Update/Delete 일괄 처리)
     * 
     * @param notifications
     * @param goodsDataSourceNo
     * @param user
     * @return
     */
    @Transactional
    public List<NotificationRes> updateNotification(NotificationReq.Update notifications, Integer goodsDataSourceNo,
            User user) {
        // 1. GoodsDataSourceDetail 조회
        GoodsDataSourceDetail goodsDataSourceDetail = goodsDataSourceDetailRepository
                .findByGoodsDataSourceNoAndIsDeletedFalse(goodsDataSourceNo)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                        "GoodsCriticalValueSetting not found"));

        // 2. DB에서 기존 GoodsCriticalValueSetting 조회 (user와 goodsDataSourceNo로 필터링)
        List<GoodsCriticalValueSetting> existingSettings = goodsCriticalValueSettingRepository
                .findByGoodsDataSourceDetailAndUserAndIsDeletedFalse(goodsDataSourceDetail, user);

        // 3. DTO에서 받은 설정 목록
        List<NotificationReq.UpdateSetting> requestedSettings = notifications.settings();

        // 4. DB와 DTO의 키(userGoodsNotificationSettingNo)로 비교 준비
        Map<Integer, GoodsCriticalValueSetting> existingSettingsMap = existingSettings.stream()
                .collect(Collectors.toMap(
                        GoodsCriticalValueSetting::getUserGoodsNotificationSettingNo,
                        setting -> setting));

        Map<Integer, NotificationReq.UpdateSetting> requestedSettingsMap = requestedSettings.stream()
                .collect(Collectors.toMap(
                        NotificationReq.UpdateSetting::userGoodsNotificationSettingNo,
                        setting -> setting));

        // 5. 삭제 처리: DB에 있는데 DTO에 없는 경우
        List<GoodsCriticalValueSetting> toDelete = existingSettings.stream()
                .filter(setting -> !requestedSettingsMap.containsKey(setting.getUserGoodsNotificationSettingNo()))
                .toList();
        toDelete.forEach(GoodsCriticalValueSetting::softDelete);

        // 6. 삽입 및 업데이트 처리
        List<GoodsCriticalValueSetting> toSave = new ArrayList<>();
        for (NotificationReq.UpdateSetting requested : requestedSettings) {
            Integer settingNo = requested.userGoodsNotificationSettingNo();
            GoodsCriticalValueSetting existing = existingSettingsMap.get(settingNo);

            if (existing == null) {
                // DB에 없음 -> 삽입
                GoodsCriticalValueSetting newSetting = GoodsCriticalValueSetting.of(
                        requested, goodsDataSourceDetail, user);
                toSave.add(newSetting);
            } else {
                // DB에 있음 -> 업데이트
                existing.update(requested);
                toSave.add(existing);
            }
        }

        // 7. 변경 사항 저장
        List<GoodsCriticalValueSetting> savedSettings = goodsCriticalValueSettingRepository.saveAll(toSave);

        // 8. 결과 반환
        return savedSettings.stream()
                .map(NotificationRes::of)
                .collect(Collectors.toList());
    }

    /**
     * 알림 삭제 (미사용)
     * 
     * @param goodsDataSourceNo
     * @param user
     * @return
     */
    @Transactional
    public List<NotificationRes> deleteNotification(NotificationReq.Delete notifications, Integer goodsDataSourceNo,
            User user) {
        GoodsDataSourceDetail goodsDataSourceDetail = goodsDataSourceDetailRepository
                .findByGoodsDataSourceNoAndIsDeletedFalse(goodsDataSourceNo)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                        "GoodsCriticalValueSetting not found"));

        List<GoodsCriticalValueSetting> goodsCriticalValueSettings = goodsCriticalValueSettingRepository
                .findByGoodsDataSourceDetailAndUserAndIsDeletedFalse(goodsDataSourceDetail, user);
        goodsCriticalValueSettings.forEach(GoodsCriticalValueSetting::softDelete);

        return goodsCriticalValueSettings.stream()
                .map(NotificationRes::of)
                .collect(Collectors.toList());
    }
}
