package com.sk.domain.shared.repository.master.tmd.custom;

import java.util.List;

import org.springframework.data.domain.Page;

import com.sk.domain.mi.dto.response.notification.MiNotificationSettingGoodsDataSourceRes;
import com.sk.domain.mi.dto.response.notification.MiNotificationSettingRes;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.entity.master.tmd.User;

public interface CustomGoodsCriticalValueSettingRepository {

    Page<MiNotificationSettingRes> findNotificationSettingsByConditions(GoodsDataSourceDetail goodsDataSourceDetail, User user, int page, int size);

    Page<MiNotificationSettingGoodsDataSourceRes> findNotificationSettingGoodsDataSourceByConditions(int page, int size, String search, User user);

    /** My Notifications 상세 조회 — 지정 데이터소스만, 조건·프로젝션은 알림 설정한 정형 데이터 목록과 동일 */
    List<MiNotificationSettingGoodsDataSourceRes> findNotificationSettingGoodsDataSourceByGoodsDataSourceNos(List<Integer> goodsDataSourceNos, User user);

}
