package com.sk.domain.shared.repository.master.tmd.custom;

import org.springframework.data.domain.Page;

import com.sk.domain.mi.dto.response.notification.MiNotificationSettingRes;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.entity.master.tmd.User;

public interface CustomGoodsCriticalValueSettingRepository {

    Page<MiNotificationSettingRes> findNotificationSettingsByConditions(GoodsDataSourceDetail goodsDataSourceDetail, User user, int page, int size);
}
