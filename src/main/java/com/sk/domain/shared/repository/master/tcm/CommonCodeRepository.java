package com.sk.domain.shared.repository.master.tcm;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.sk.domain.shared.entity.master.tcm.CommonCode;

public interface CommonCodeRepository extends JpaRepository<CommonCode, String> {

    @Query(value = """
            SELECT cc
            FROM CommonCode cc
            WHERE cc.isDeleted = false
                AND cc.isUsed = true
                AND cc.codeGroupId = :codeGroupId
                AND cc.useBeginDt <= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
                AND cc.useEndDt >= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
                        """)
    Optional<CommonCode> findByCodeGroupId(String codeGroupId);
}
