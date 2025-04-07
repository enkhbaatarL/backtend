package com.sk.domain.shared.repository.master.tmd;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.domain.shared.entity.master.tmd.GoodsCriticalValueSetting;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.entity.master.tmd.User;

public interface GoodsCriticalValueSettingRepository extends JpaRepository<GoodsCriticalValueSetting, Integer> {

    @EntityGraph(attributePaths = { "goodsDataSourceDetail", "user" })
    List<GoodsCriticalValueSetting> findAllByIsDeletedFalse();

    List<GoodsCriticalValueSetting> findAllByGoodsDataSourceDetailAndUserAndIsDeletedFalse(
            GoodsDataSourceDetail goodsDataSourceDetail, User user);
    
    Optional<GoodsCriticalValueSetting> findByUserGoodsNotificationSettingNoAndUserAndIsDeletedFalse(
            Integer userGoodsNotificationSettingNo, User user);
}
