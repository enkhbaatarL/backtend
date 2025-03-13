package com.sk.domain.admin.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.request.CustomerCompanyReq;
import com.sk.domain.admin.dto.response.CustomerCompanyRes;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminCompanyInfoService {

    private final CustomerCompanyRepository customerCompanyRepository;

    /**
     * 고객사 목록을 페이지네이션 처리하여 조회한다.
     *
     * @param page 조회할 페이지 번호 (min 1)
     * @param size 조회할 요소 사이즈 (min 1)
     * @return PaginationRes<CustomerCompanyRes>
     */
    public PaginationRes<CustomerCompanyRes> getCompanies(int page, int size) {
        return PaginationRes.toRes(customerCompanyRepository.findCompaniesInfoByConditions(page, size));
    }

    /**
     * 고객사를 등록한다.
     *
     * @param customerCompanyReq 고객사 등록 데이터
     * @return CustomerCompanyRes
     */
    @Transactional
    public CustomerCompanyRes postCompany(CustomerCompanyReq customerCompanyReq) {

        customerCompanyRepository.findByCustomerCompanyCdAndIsDeletedFalse(customerCompanyReq.companyCd())
            .ifPresent(company -> {
                throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "already exists.");
            });

        CustomerCompany savedEntity = customerCompanyRepository.save(CustomerCompany.of(customerCompanyReq));

        return CustomerCompanyRes.of(savedEntity);
    }

}
