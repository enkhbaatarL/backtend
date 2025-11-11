package com.sk.domain.mi.service;

import com.sk.domain.mi.dto.response.MiCompanyDetailRes;
import com.sk.domain.mi.dto.response.MiCompanyRes;
import com.sk.domain.shared.entity.master.tmd.CustomerCompanyMarketCategory;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyMarketCategoryRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiCompanyService {

    private final CustomerCompanyRepository customerCompanyRepository;
    private final CustomerCompanyMarketCategoryRepository customerCompanyMarketCategoryRepository;

    /**
     * 고객사 회사 목록 조회
     *
     * @return 고객사 회사 목록
     */
    public List<MiCompanyRes> getCompanyList() {
        return customerCompanyRepository.findAllMiCompanies();
    }

    public MiCompanyDetailRes getCompanyDetail(Integer companyNo) {
        MiCompanyDetailRes miCompanyDetailRes = customerCompanyRepository.findMiCompany(companyNo);
        CustomerCompanyMarketCategory customerCompanyMarketCategory = customerCompanyMarketCategoryRepository.findFirstByCustomerCompanyNoAndIsDeletedFalse(
            companyNo).orElse(null);

        if (customerCompanyMarketCategory != null) {
            miCompanyDetailRes = miCompanyDetailRes.toBuilder()
                .newsMarketCategoryCode(customerCompanyMarketCategory.getNwsMrktCtgrCd())
                .build();
        }

        return miCompanyDetailRes;
    }
}
