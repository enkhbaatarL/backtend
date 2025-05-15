package com.sk.domain.shared.repository.master.tcm;

import com.sk.domain.shared.entity.master.tcm.PolicyTactics;
import com.sk.domain.shared.enums.PolicyTacticsSeparateCd;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PolicyTacticsRepository extends JpaRepository<PolicyTactics, Integer> {

    List<PolicyTactics> findByPoltacSeparateCdAndIsUsedTrueAndIsDeletedFalseOrderByCreateDtDesc(PolicyTacticsSeparateCd policyTacticsSeparateCd);

    Optional<PolicyTactics> findByPoltacNoAndPoltacSeparateCdAndIsUsedTrueAndIsDeletedFalse(Integer poltacNo, PolicyTacticsSeparateCd policyTacticsSeparateCd);

    Optional<PolicyTactics> findFirstByPoltacSeparateCdAndIsUsedTrueAndIsDeletedFalseOrderByCreateDtDesc(PolicyTacticsSeparateCd policyTacticsSeparateCd);

}
