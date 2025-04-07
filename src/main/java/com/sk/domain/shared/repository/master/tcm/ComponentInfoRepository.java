package com.sk.domain.shared.repository.master.tcm;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.domain.shared.entity.master.tcm.ComponentInfo;
import com.sk.domain.shared.repository.master.tcm.custom.CustomComponentInfoRepository;

public interface ComponentInfoRepository extends JpaRepository<ComponentInfo, String>, CustomComponentInfoRepository {
    Optional<ComponentInfo> findByComponentTypeIdAndIsDeletedFalse(String componentTypeId);
    
    List<ComponentInfo> findByIsDeletedFalse();
}

