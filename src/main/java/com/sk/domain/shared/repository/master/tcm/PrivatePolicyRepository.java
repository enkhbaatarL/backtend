package com.sk.domain.shared.repository.master.tcm;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.sk.domain.admin.dto.response.privatePolicy.PrivatePolicyRes;
import com.sk.domain.shared.entity.master.tcm.PrivatePolicy;

public interface PrivatePolicyRepository extends JpaRepository<PrivatePolicy, Integer> {

    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.privatePolicy.PrivatePolicyRes(
            p.poltacNo,
            p.poltacVersionNm
            )
            FROM PrivatePolicy p
            WHERE p.isDeleted = false
            ORDER BY p.createDt ASC
            """)
    List<PrivatePolicyRes> findByIsDeletedFalseAndIsUsedTrue();

    Optional<PrivatePolicy> findTop1ByIsDeletedFalseOrderByCreateDtDesc();

    Optional<PrivatePolicy> findByPoltacNoAndIsDeletedFalseAndIsUsedTrue(Integer poltacNo);

}
