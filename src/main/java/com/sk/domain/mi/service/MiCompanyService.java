package com.sk.domain.mi.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.domain.mi.dto.response.MiCompanyRes;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiCompanyService {

    private final CustomerCompanyRepository customerCompanyRepository;

    /**
     * 고객사 회사 목록 조회
     * 
     * @return 고객사 회사 목록
     */
    public List<MiCompanyRes> getCompanyList() {
        return customerCompanyRepository.findAllMiCompanies();
    }
}
