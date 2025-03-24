package com.sk.domain.shared.repository.master.tcm;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.domain.shared.entity.master.tcm.PrivatePolicy;

public interface PrivatePolicyRepository extends JpaRepository<PrivatePolicy, Integer> {

    List<PrivatePolicy> findByIsDeletedFalseAndisUsedTrue();

    Optional<PrivatePolicy> findByPoltacNoAndIsDeletedFalseAndisUsedTrue(Integer poltacNo);
}
