package com.sk.domain.shared.repository.master.tcm;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.sk.domain.admin.dto.response.privatePolicy.PrivatePolicyRes;
import com.sk.domain.shared.entity.master.tcm.PrivatePolicy;

public interface PrivatePolicyRepository extends JpaRepository<PrivatePolicy, Integer> {

    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.privatePolicyPrivatePolicyRes(
            p.poltacNo,
            p.poltacVersionNm
            )
            FROM PrivatePolicy p
            WHERE b.isDeleted = false
            ORDER BY b.createDt ASC
            """)
    List<PrivatePolicyRes> findByIsDeletedFalseAndisUsedTrue();

    Optional<PrivatePolicy> find1ByIsDeletedFalseOrderByCreateDtDesc();

    Optional<PrivatePolicy> findByPoltacNoAndIsDeletedFalseAndisUsedTrue(Integer poltacNo);

}
