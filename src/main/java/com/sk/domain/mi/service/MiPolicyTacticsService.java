package com.sk.domain.mi.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.response.tactics.PolicyTacticsRes;
import com.sk.domain.mi.dto.response.tactics.PolicyTacticsSimpleRes;
import com.sk.domain.shared.entity.master.tcm.PolicyTactics;
import com.sk.domain.shared.enums.PolicyTacticsSeparateCd;
import com.sk.domain.shared.repository.master.tcm.PolicyTacticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiPolicyTacticsService {

    private final PolicyTacticsRepository policyTacticsRepository;

    /**
     * 모든 개인정보 처리방침 리스트를 조회합니다.
     *
     * @return 최근 생성일 기준으로 정렬된 개인정보 처리방침 리스트
     */
    public List<PolicyTacticsSimpleRes> getPrivacyPolicies() {
        List<PolicyTactics> policyTactics = policyTacticsRepository.findByPoltacSeparateCdAndIsUsedTrueAndIsDeletedFalseOrderByCreateDtDesc(PolicyTacticsSeparateCd.PRIVACY_POLICY);
        return PolicyTacticsSimpleRes.ofList(policyTactics);
    }

    /**
     * 특정 개인정보 처리방침의 상세 정보를 조회합니다.
     *
     * @param poltacNo 개인정보 처리방침 번호
     * @return 해당 번호에 해당하는 개인정보 처리방침의 상세 정보
     * @throws AppErrorException 해당 번호에 대한 개인정보 처리방침 정보가 없을 경우 예외 발생
     */
    public PolicyTacticsRes getPrivacyPoliciesDetail(Integer poltacNo) {
        PolicyTactics policyTactics = policyTacticsRepository.findByPoltacNoAndPoltacSeparateCdAndIsUsedTrueAndIsDeletedFalse(poltacNo, PolicyTacticsSeparateCd.PRIVACY_POLICY)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [policy-tactics]. poltacNo: %s".formatted(poltacNo)));
        return PolicyTacticsRes.of(policyTactics);
    }

    /**
     * 최신 개인정보 처리방침의 상세 정보를 조회합니다.
     *
     * @return 생성일 기준 가장 최신 개인정보 처리방침의 상세 정보
     */
    public PolicyTacticsRes getLatestPrivacyPoliciesDetail() {
        return policyTacticsRepository.findFirstByPoltacSeparateCdAndIsUsedTrueAndIsDeletedFalseOrderByCreateDtDesc(PolicyTacticsSeparateCd.PRIVACY_POLICY)
            .map(PolicyTacticsRes::of)
            .orElse(null);
    }

}
