package com.sk.domain.shared.repository.master.tmm;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.domain.shared.entity.master.tmm.AssetAllocationInfo;
import com.sk.domain.shared.entity.compositeKey.tmm.AssetAllocationInfoId;

public interface AssetAllocationInfoRepository extends JpaRepository<AssetAllocationInfo, AssetAllocationInfoId> {

}
