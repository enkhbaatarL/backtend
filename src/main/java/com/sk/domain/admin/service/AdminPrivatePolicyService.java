package com.sk.domain.admin.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.admin.dto.response.privatePolicy.PrivatePolicyRes;
import com.sk.domain.admin.dto.response.privatePolicy.PrivatePolicyVersionRes;
import com.sk.domain.shared.entity.master.tcm.PrivatePolicy;
import com.sk.domain.shared.repository.master.tcm.PrivatePolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminPrivatePolicyService {
    private final PrivatePolicyRepository policyRepository;

    // 버전명 리스트
    // 버전명으로 정책약관 불러오기

    /**
    * 버전명 리스트
    *
    * @param
    * @return List<PrivatePolicyRes>
    */
    public List<PrivatePolicyVersionRes> getPrivatePolicies() {
        return policyRepository.findByIsDeletedFalseAndIsUsedTrueOrderByCreateDtAsc()
                .stream()
                .map(PrivatePolicyVersionRes::of)
                .collect(Collectors.toList());
    }

    /**
    * 정책약관 불러오기
    *
    * @param poltacNo
    * @return PrivatePolicy
    */
    public PrivatePolicyRes getPrivatePolicy(Integer poltacNo) {
        PrivatePolicy privatePolicy = policyRepository.findByPoltacNoAndIsDeletedFalseAndIsUsedTrue(poltacNo)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("poltac no: %d", poltacNo)));

        return PrivatePolicyRes.of(privatePolicy);
    }

    /**
    * 최신 정책 약관 불러오기
    *
    * @param
    * @return PrivatePolicy
    */
    public PrivatePolicyRes getLatestPrivatePolicy() {
        PrivatePolicy privatePolicy = policyRepository.findFirstByIsDeletedFalseAndIsUsedTrueOrderByCreateDtDesc()
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND));

        return PrivatePolicyRes.of(privatePolicy);
    }

}
