package com.sk.domain.shared.repository.master.tcm;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.sk.domain.admin.dto.response.provider.ComponentInfoRes;
import com.sk.domain.shared.entity.master.tcm.ComponentInfo;
import com.sk.domain.shared.repository.master.tcm.custom.CustomComponentInfoRepository;

public interface ComponentInfoRepository extends JpaRepository<ComponentInfo, String>, CustomComponentInfoRepository {
    Optional<ComponentInfo> findByComponentTypeIdAndIsDeletedFalse(String componentTypeId);

    List<ComponentInfo> findByIsDeletedFalse();

    @Query(value = """
                SELECT new com.sk.domain.admin.dto.response.provider.ComponentInfoRes(
                    c.componentTypeId,
                    c.componentTypeNm
                )
                FROM ComponentInfo c
                WHERE c.isDeleted = false
                ORDER BY c.componentTypeNm ASC
            """)
    List<ComponentInfoRes> findComponentNm();
}
