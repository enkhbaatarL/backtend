package com.sk.domain.shared.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.domain.shared.entity.master.tif.UserInfo;
import com.sk.domain.shared.entity.master.tmd.GoodsCriticalValueSetting;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.repository.master.tif.UserInfoRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsCriticalValueSettingRepository;
import com.sk.domain.shared.repository.master.tmd.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 스케쥴러용 서비스
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SchedulerService {
    private final UserRepository userRepository;
    private final UserInfoRepository userInfoRepository;
    private final GoodsCriticalValueSettingRepository goodsCriticalValueSettingRepository;

    /**
     * GDI 탈퇴 유저 soft delete 처리
     */
    @Transactional
    public Boolean processGdiUserSoftDelete() {
        // 인터페이스 테이블에서 매번 삭제된 유저 조회 하는데 User 테이블에서 삭제된 유저 상관 없이 조회 하기 때문에 비효율적
        List<UserInfo> deletedUserInfos = userInfoRepository.findByIsDeletedTrue();

        if(deletedUserInfos.isEmpty()) {
            return true;
        }

        List<User> users = userRepository.findByUserInfosAndIsDeletedFalse(deletedUserInfos);

        if(!users.isEmpty()) {
            users.stream()
                    .peek(User::softDelete)
                    .collect(Collectors.toList());
        }

        return true;
}

    /**
     * 임계치를 확인하고 알림을 발송한다.
     * 
     * @return 알림 발송 결과
     */
    @Transactional
    public Boolean createNotification() {
        List<GoodsCriticalValueSetting> goodsCriticalValueSettings = goodsCriticalValueSettingRepository
                .findAllByIsDeletedFalse();

        for (GoodsCriticalValueSetting goodsCriticalValueSettingData : goodsCriticalValueSettings) {
            // TODO: 호영: 임계치에 따른 이메일 발송 로직 추가, 성공한 메일, 실패한 메일 나눠서 Response 작성

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