package com.sk.domain.admin.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.SortUtil;
import com.sk.domain.admin.dto.request.ProviderMappingReq;
import com.sk.domain.admin.dto.request.CustomerCompanyReq;
import com.sk.domain.admin.dto.response.*;
import com.sk.domain.shared.entity.master.tam.CustomerCompanyGoodsMapping;
import com.sk.domain.shared.entity.master.tam.CustomerCompanyProviderMapping;
import com.sk.domain.shared.entity.master.tif.CompanyInfo;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.Provider;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.repository.master.tam.CustomerCompanyGoodsMappingRepository;
import com.sk.domain.shared.repository.master.tam.CustomerCompanyProviderMappingRepository;
import com.sk.domain.shared.repository.master.tif.CompanyInfoRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.repository.master.tmd.ProviderRepository;
import com.sk.domain.shared.repository.master.tmd.UserRepository;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminCompanyService {

    private final UserRepository userRepository;
    private final ProviderRepository providerRepository;
    private final CompanyInfoRepository companyInfoRepository;
    private final CustomerCompanyRepository customerCompanyRepository;
    private final CustomerCompanyProviderMappingRepository customerCompanyProviderMappingRepository;
    private final CustomerCompanyGoodsMappingRepository customerCompanyGoodsMappingRepository;

    /**
     * 그룹사 목록을 조회한다.
     * - sort : companyNm ASC (숫자 -> 알파벳 -> 한글)
     *
     * @return List<GroupCompany>
     */
    public List<GroupCompanyRes> getGroupCompanies() {
        List<CompanyInfo> groupCompanies = companyInfoRepository.findByIsDeletedFalse();
        return GroupCompanyRes.ofListOrderByAsc(groupCompanies, SortUtil.naturalComparator(CompanyInfo::getCompanyNm));
    }

    /**
     * 고객사 목록을 페이지네이션 처리하여 조회한다.
     *   - sort : createDt DESC
     *
     * @param page 조회할 페이지 번호 (min 1)
     * @param size 조회할 요소 사이즈 (min 1)
     * @param companyNm 고객사 명 (LIKE '%검색어%)
     * @return PaginationRes<CustomerCompanyRes>
     */
    public PaginationRes<CompanyRes> getCompanies(int page, int size, String companyNm) {
        return PaginationRes.toRes(customerCompanyRepository.findCompaniesInfoByConditions(page, size, companyNm));
    }

    /**
     * 고객사를 등록한다.
     *
     * @param customerCompanyReq 고객사 등록 데이터
     * @return CustomerCompanyRes
     */
    @Transactional
    public CompanyRes postCompany(CustomerCompanyReq customerCompanyReq) {

        // 고객사 유효성 체크
        customerCompanyRepository.findByCustomerCompanyCdAndIsDeletedFalse(customerCompanyReq.companyCd())
            .ifPresent(company -> {
                throw AppErrorException.of(ResultCode.Error.RSC_ALREADY_EXISTS,
                    "companyCd : %s".formatted(customerCompanyReq.companyCd()));
            });

        // 그룹사 유효성 체크
        companyInfoRepository.findByCompanyCdAndIsDeletedFalse(customerCompanyReq.companyCd())
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "It doesn't exist in the group company. companyCd: %s".formatted(customerCompanyReq.companyCd())));


        CustomerCompany savedEntity = customerCompanyRepository.save(CustomerCompany.of(customerCompanyReq));

        return CompanyRes.of(savedEntity);
    }

    /**
     * 고객사 상세 구성을 조회한다.
     * [구성요소]
     *   - 고객사
     *   - 프로바이더 목록(with 구독중인 상품 개수)
     *   - 회원 목록
     *
     * @param companyNo 고객회사번호
     * @return CompanyOverviewRes
     */
    public CompanyOverviewRes getCompanyOverview(Integer companyNo) {

        // 고객사 조회 및 유효성 체크
        CustomerCompany company = customerCompanyRepository.findByCustomerCompanyNoAndIsDeletedFalse(companyNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "It doesn't exist in the company. companyNo: %s".formatted(companyNo)));

        List<User> users = userRepository.findByCustomerCompanyAndIsDeletedFalse(company);
        List<CustomerCompanyProviderMapping> providerMappings = customerCompanyProviderMappingRepository.findByCustomerCompanyAndIsDeletedFalse(company);
        List<CustomerCompanyGoodsMapping> goodsMappings = customerCompanyGoodsMappingRepository.findByCustomerCompanyAndIsDeletedFalse(company);

        return CompanyOverviewRes.of(company, users, providerMappings, goodsMappings);

    }

    /**
     * 고객사가 구독중인 프로바이더 목록을 페이지네이션 처리하여 조회한다. (with 구독중인 상품 개수)
     *   - sort : createDt DESC
     *
     * @param companyNo 고객회사번호
     * @param page 조회할 페이지 번호 (min 1)
     * @param size 조회할 요소 사이즈 (min 1)
     * @return PaginationRes<ProviderMappingGoodsRes>
     */
    public PaginationRes<ProviderMappingGoodsRes> getCompanyProviders(Integer companyNo, int page, int size) {
        return PaginationRes.toRes(customerCompanyProviderMappingRepository.findCompaniesMappingsByConditions(page, size, companyNo));
    }

    /**
     * 고객회사와 프로바이더를 매핑시켜 등록한다.
     *
     * @param companyNo 고객회사번호
     * @param providerMappingReq 고객-프로바이더 DTO
     * @return ProviderMappingRes
     */
    @Transactional
    public ProviderMappingRes postCompanyProvider(Integer companyNo, ProviderMappingReq providerMappingReq) {

        // 고객사 조회 및 유효성 체크
        CustomerCompany company = customerCompanyRepository.findByCustomerCompanyNoAndIsDeletedFalse(companyNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "It doesn't exist in the company. companyNo: %s".formatted(companyNo)));

        // 프로바이더 조회 및 유효성 체크
        Provider provider = providerRepository.findByProviderNoAndIsDeletedFalse(providerMappingReq.providerNo())
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "It doesn't exist in the provider. providerNo: %s".formatted(providerMappingReq.providerNo())));

        // 고객사-프로바이더 유효성 체크
        customerCompanyProviderMappingRepository.findByCustomerCompanyAndProviderAndIsDeletedFalse(company, provider)
            .ifPresent(mapping -> {
                throw AppErrorException.of(ResultCode.Error.RSC_ALREADY_EXISTS,
                    "companyNo : %s, providerNo : %s".formatted(companyNo, providerMappingReq.providerNo()));
            });

        CustomerCompanyProviderMapping providerMapping = customerCompanyProviderMappingRepository.save(CustomerCompanyProviderMapping.of(company, provider));

        return ProviderMappingRes.of(providerMapping);
    }

    /**
     * 고객사와 부서 목록을 조회한다.
     *
     * @return List<CompanyDepartmentRes>
     */
    public List<CompanyDepartmentRes> getCompaniesAndDepartments() {
        return customerCompanyRepository.findAllCompaniesAndDepartments();
    }
    
}
