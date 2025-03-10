package com.sk.domain.admin.service;

import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.request.CompanyInfoReq;
import com.sk.domain.admin.dto.response.CompanyInfoRes;
import com.sk.domain.shared.entity.master.tif.CompanyInfo;
import com.sk.domain.shared.repository.master.tif.CompanyInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CompanyInfoService {

    private final CompanyInfoRepository companyInfoRepository;

    /**
     * 고객사 목록을 페이지네이션 처리하여 조회한다.
     *
     * @param page 조회할 페이지 번호 (min 1)
     * @param size 조회할 요소 사이즈 (min 1)
     * @return PaginationRes<CompanyInfoRes>
     */
    public PaginationRes<CompanyInfoRes> getCompaniesInfo(int page, int size) {
        return PaginationRes.toRes(companyInfoRepository.findCompaniesInfoByConditions(page, size));
    }

    /**
     * 고객사를 등록한다.
     *
     * @param companyInfoReq 고객사 등록 데이터
     * @return CompanyInfoRes
     */
    @Transactional
    public CompanyInfoRes postCompany(CompanyInfoReq companyInfoReq) {
        CompanyInfo savedEntity = companyInfoRepository.save(CompanyInfo.of(companyInfoReq));
        return CompanyInfoRes.of(savedEntity);
    }

}
