package com.sk.domain.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public String getPrivatePolicies() {
        return "todo";
    }

    public String getPrivatePolicy(Integer poltacNo) {
        return "todo";
    }
}
