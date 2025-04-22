package com.sk.domain.shared.repository.master.tmd;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.domain.shared.entity.master.tmd.GoodsCriticalValueSetting;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.repository.master.tmd.custom.CustomGoodsCriticalValueSettingRepository;

public interface GoodsCriticalValueSettingRepository
        extends JpaRepository<GoodsCriticalValueSetting, Integer>, CustomGoodsCriticalValueSettingRepository {

    @EntityGraph(attributePaths = { "goodsDataSourceDetail", "goodsDataSourceDetail.goodsData", "user" })
    List<GoodsCriticalValueSetting> findAllByIsDeletedFalse();

    Optional<GoodsCriticalValueSetting> findByUserGoodsNotificationSettingNoAndUserAndIsDeletedFalse(
            Integer userGoodsNotificationSettingNo, User user);
}
