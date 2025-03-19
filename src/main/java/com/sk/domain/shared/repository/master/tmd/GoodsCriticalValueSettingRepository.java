package com.sk.domain.shared.repository.master.tmd;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.domain.shared.entity.master.tmd.GoodsCriticalValueSetting;

public interface GoodsCriticalValueSettingRepository extends JpaRepository<GoodsCriticalValueSetting, Integer> {

    List<GoodsCriticalValueSetting> findAllByIsDeletedFalse();
}
