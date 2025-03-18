package com.sk.domain.admin.service;

import com.sk.common.model.PaginationRes;
import com.sk.common.model.ValidationResult;
import com.sk.common.utils.SortUtil;
import com.sk.domain.admin.dto.request.company.AuthGoodsMapReq;
import com.sk.domain.admin.dto.request.company.CompanyGoodsMapReq;
import com.sk.domain.admin.dto.request.company.CompanyProviderMapReq;
import com.sk.domain.admin.dto.request.company.CompanyReq;
import com.sk.domain.admin.dto.response.CompanyDepartmentRes;
import com.sk.domain.admin.dto.response.company.*;
import com.sk.domain.admin.validator.CompanyValidator;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.master.tam.AuthManageGoodsMapping;
import com.sk.domain.shared.entity.master.tam.CustomerCompanyGoodsMapping;
import com.sk.domain.shared.entity.master.tam.CustomerCompanyProviderMapping;
import com.sk.domain.shared.entity.master.tif.CompanyInfo;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.Goods;
import com.sk.domain.shared.entity.master.tmd.Provider;
import com.sk.domain.shared.enums.GoodsAuthCd;
import com.sk.domain.shared.enums.GoodsAuthManageSupportCd;
import com.sk.domain.shared.repository.master.tam.AuthManageGoodsMappingRepository;
import com.sk.domain.shared.repository.master.tam.CustomerCompanyGoodsMappingRepository;
import com.sk.domain.shared.repository.master.tam.CustomerCompanyProviderMappingRepository;
import com.sk.domain.shared.repository.master.tif.CompanyInfoRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
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

    private final CompanyValidator companyValidator;

    private final CompanyInfoRepository companyInfoRepository;
    private final CustomerCompanyRepository customerCompanyRepository;
    private final AuthManageGoodsMappingRepository authManageGoodsMappingRepository;
    private final CustomerCompanyGoodsMappingRepository customerCompanyGoodsMappingRepository;
    private final CustomerCompanyProviderMappingRepository customerCompanyProviderMappingRepository;

    /**
     * 그룹사 목록을 조회한다.
     * - Sort: companyNm ASC
     * - Priority: 숫자 > 알파벳 > 한글
     *
     * @return List<GroupCompany>
     */
    public List<GroupCompanyRes> getGroupCompanies() {
        List<CompanyInfo> groupCompanies = companyInfoRepository.findByIsDeletedFalse();
        return GroupCompanyRes.ofListOrderByAsc(groupCompanies, SortUtil.naturalComparator(CompanyInfo::getCompanyNm));
    }

    /**
     * 고객사 목록을 조회한다. (with pagination)
     * - Sort : companyNm ASC
     *
     * @param page      조회할 페이지 번호 (min 1)
     * @param size      조회할 요소 사이즈 (min 1)
     * @param companyNm 고객사 명 (LIKE '%검색어%)
     * @return PaginationRes<CustomerCompanyRes>
     */
    public PaginationRes<CompanyRes> getCompanies(int page, int size, String companyNm) {
        return PaginationRes.toRes(customerCompanyRepository.findCompaniesByConditions(page, size, companyNm));
    }

    /**
     * 고객사를 등록한다.
     *
     * @param companyReq 고객사 등록 데이터
     * @return CustomerCompanyRes
     */
    @Transactional
    public CompanyRes postCompany(CompanyReq companyReq) {

        companyValidator.ensureCompanyNotExistsByCompanyCd(companyReq.companyCd());
        companyValidator.ensureGroupCompanyExistsByCompanyCd(companyReq.companyCd());

        CustomerCompany savedEntity = customerCompanyRepository.save(CustomerCompany.of(companyReq));

        return CompanyRes.of(savedEntity);

    }

    /**
     * 고객사를 상세 조회한다.
     *
     * @param companyNo 고객회사 번호
     * @return CompanyRes
     */
    public CompanyRes getCompanyDetail(Integer companyNo) {
        ValidationResult<CustomerCompany> valid = companyValidator.ensureCompanyExistsByCompanyNo(companyNo);
        CustomerCompany company = valid.getOrThrow();
        return CompanyRes.of(company);
    }

    /**
     * 고객사와 매핑된 프로바이더 목록을 조회한다. 해당하는 프로바이더의 상품 리스트 중에 고객사가 구독중인 상품 개수를 함께 반환한다.
     * - Sort : providerNm ASC
     *
     * @param companyNo 고객회사 번호
     * @return List<ProviderWithGoodsCntRes>
     */
    public List<ProviderWithGoodsCntRes> getProvidersMappedToCompany(Integer companyNo) {
        return customerCompanyProviderMappingRepository.findCompanyMappingsWithGoodsCntByCompanyNo(companyNo);
    }

    /**
     * 고객사와 프로바이더를 매핑시켜 등록한다.
     *
     * @param companyNo             고객회사 번호
     * @param companyProviderMapReq 매핑DTO
     * @return ProviderMappingRes
     */
    @Transactional
    public CompanyProviderMapRes postProviderMappedToCompany(Integer companyNo,
            CompanyProviderMapReq companyProviderMapReq) {

        ValidationResult<CustomerCompany> companyValid = companyValidator.ensureCompanyExistsByCompanyNo(companyNo);
        CustomerCompany company = companyValid.getOrThrow();

        ValidationResult<Provider> providerValid = companyValidator
                .ensureProviderExistsByProviderNo(companyProviderMapReq.providerNo());
        Provider provider = providerValid.getOrThrow();

        companyValidator.ensureCcpmNotExistsByCompanyAndProvider(company, provider);

        return CompanyProviderMapRes.of(
                customerCompanyProviderMappingRepository.save(CustomerCompanyProviderMapping.of(company, provider)));

    }

    /**
     * 고객사와 매핑되어 있는 프로바이더를 상세 조회한다.
     *
     * @param companyNo  고객회사 번호
     * @param providerNo 프로바이더 번호
     * @return ProviderMappingDetailRes
     */
    public CompanyProviderMapDetailRes getProviderDetail(Integer companyNo, Integer providerNo) {

        ValidationResult<CustomerCompanyProviderMapping> ccpValid = companyValidator
                .ensureCcpmExistsByCompanyNoAndProviderNo(companyNo, providerNo);
        CustomerCompanyProviderMapping ccp = ccpValid.getOrThrow();

        return CompanyProviderMapDetailRes.of(ccp);

    }

    /**
     * 고객사와 매핑된 프로바이더의 상품 목록중에 고객사가 구독한 상품 목록을 조회한다. (with pagination)
     * - Sort : goodsNm ASC
     *
     * @param page       조회할 페이지 번호 (min 1)
     * @param size       조회할 요소 사이즈 (min 1)
     * @param companyNo  고객회사 번호
     * @param providerNo 프로바이더 번호
     * @return PaginationRes<GoodsWithDataCntRes>
     */
    public PaginationRes<GoodsWithDataCntRes> getGoodsMappedToCompany(int page, int size, Integer companyNo,
            Integer providerNo) {
        return PaginationRes.toRes(
                customerCompanyGoodsMappingRepository.findGoodsMappingsByConditions(page, size, companyNo, providerNo));
    }

    /**
     * 고객사와 상품을 매핑하여 등록한다.
     *
     * @param companyNo          고객회사 번호
     * @param providerNo         프로바이더 번호
     * @param companyGoodsMapReq 매핑DTO
     * @return GoodsMappingRes
     */
    @Transactional
    public CompanyGoodsMapRes postGoodsMappedToCompany(Integer companyNo, Integer providerNo,
            CompanyGoodsMapReq companyGoodsMapReq) {

        ValidationResult<CustomerCompany> companyValid = companyValidator.ensureCompanyExistsByCompanyNo(companyNo);
        CustomerCompany company = companyValid.getOrThrow();

        ValidationResult<CustomerCompanyProviderMapping> ccpValid = companyValidator
                .ensureCcpmExistsByCompanyNoAndProviderNo(companyNo, providerNo);
        CustomerCompanyProviderMapping ccp = ccpValid.getOrThrow();

        ValidationResult<CustomerCompanyGoodsMapping> ccgValid = companyValidator
                .ensureCcgmNotExistsByCompanyNoAndGoodsNo(companyNo,
                        companyGoodsMapReq.goodsNo());
        CustomerCompanyGoodsMapping ccg = ccgValid.getOrThrow();

        ValidationResult<Goods> goodsValid = companyValidator.ensureGoodsExistsByGoodsNo(providerNo,
                companyGoodsMapReq.goodsNo());
        Goods goods = goodsValid.getOrThrow();

        return CompanyGoodsMapRes.of(customerCompanyGoodsMappingRepository
                .save(CustomerCompanyGoodsMapping.of(company, goods, companyGoodsMapReq)));
    }

    /**
     * 고객사와 매핑되어 있는 상품을 상세 조회한다.
     *
     * @param companyNo  고객회사 번호
     * @param providerNo 프로바이더 번호
     * @param goodsNo    상품 번호
     * @return GoodsMappingDetailRes
     */
    public CompanyGoodsMapDetailRes getGoodsDetail(Integer companyNo, Integer providerNo, Integer goodsNo) {

        ValidationResult<CustomerCompanyProviderMapping> ccpValid = companyValidator
                .ensureCcpmExistsByCompanyNoAndProviderNo(companyNo, providerNo);
        CustomerCompanyProviderMapping ccp = ccpValid.getOrThrow();

        ValidationResult<CustomerCompanyGoodsMapping> ccgValid = companyValidator
                .ensureCcgmExistsByCompanyNoAndGoodsNo(companyNo, goodsNo);
        CustomerCompanyGoodsMapping ccg = ccgValid.getOrThrow();

        if (GoodsAuthManageSupportCd.COMPANY == ccg.getId().getGoodsAuthManageSupportCd()) {
            List<AuthManageGoodsMapping> companyAuth = authManageGoodsMappingRepository
                    .findByCompanyNoAndGoodsNoAndGoodsAuthCd(companyNo, goodsNo,
                            GoodsAuthManageSupportCd.COMPANY);
            return CompanyGoodsMapDetailRes.of(ccg, companyAuth);
        }

        return CompanyGoodsMapDetailRes.of(ccg);
    }

    /**
     * 고객사와 매핑되어 있는 상품에 대한 사용자 권한 설정을 조회한다. (with pagination)
     * - Sort : userNm ASC
     *
     * @param companyNo  고객회사 번호
     * @param providerNo 프로바이더 번호
     * @param goodsNo    상품 번호
     * @return PaginationRes<AuthGoodsMappingRes>
     */
    public PaginationRes<AuthGoodsMapRes> getAuthGoods(int page, int size, Integer companyNo, Integer providerNo,
            Integer goodsNo) {
        return PaginationRes
                .toRes(authManageGoodsMappingRepository.findByAuthGoodsByConditions(page, size, companyNo, goodsNo));
    }

    /**
     * 고객사와 매핑되어 있는 상품에 대한 사용자 권한을 설정한다.
     *
     * @param companyNo       고객회사 번호
     * @param providerNo      프로바이더 번호
     * @param goodsNo         상품 번호
     * @param authGoodsMapReq 권한상품DTO
     * @return AuthGoodsMappingRes
     */
    @Transactional
    public AuthGoodsMapRes patchGoodsAuth(Integer companyNo, Integer providerNo, Integer goodsNo,
            AuthGoodsMapReq authGoodsMapReq) {

        ValidationResult<CustomerCompanyProviderMapping> ccpValid = companyValidator
                .ensureCcpmExistsByCompanyNoAndProviderNo(companyNo, providerNo);
        CustomerCompanyProviderMapping ccp = ccpValid.getOrThrow();

        ValidationResult<CustomerCompanyGoodsMapping> ccgValid = companyValidator
                .ensureCcgmExistsByCompanyNoAndGoodsNo(companyNo, goodsNo);
        CustomerCompanyGoodsMapping ccg = ccgValid.getOrThrow();

        // 고객사-상품 매핑 업데이트
        ccg.update(authGoodsMapReq);

        if (authGoodsMapReq.goodsAuthManageCd() == GoodsAuthManageSupportCd.COMPANY) {

            List<AuthManageGoodsMapping> amgMappings = authManageGoodsMappingRepository
                    .findByCustomerCompanyGoodsMappingAndIsDeletedFalse(ccg);
            // 삭제
            amgMappings.forEach(BaseEntity::softDelete);

            // 뷰 권한
            if (authGoodsMapReq.companySetting().isViewable()) {
                authManageGoodsMappingRepository
                        .save(AuthManageGoodsMapping.of(authGoodsMapReq, GoodsAuthCd.VIEW, ccg));
            }

            // 다운로드 권한
            if (authGoodsMapReq.companySetting().isDownloadable()) {
                authManageGoodsMappingRepository
                        .save(AuthManageGoodsMapping.of(authGoodsMapReq, GoodsAuthCd.DOWNLOAD, ccg));
            }

        } else if (authGoodsMapReq.goodsAuthManageCd() == GoodsAuthManageSupportCd.INDIVIDUAL) {

            // todo

        }

        return null;
    }

    /**
     * 최신 수정일을 조회한다.
     * 수정일 범위
     * - 1. 고객사 마지막 수정 일시(삭제 포함)
     * - 2. 회사-프로바이더 매핑 마지막 수정 일시(삭제 포함)
     * - 3. 사용자 마지막 수정 일시(삭제 포함)
     *
     * @param companyNo 고객회사 번호
     * @return LatestUpdatedRes
     */
    public LatestUpdatedRes getCompanyLatestUpdated(Integer companyNo) {
        return customerCompanyRepository.findLatestUpdated(companyNo);
    }

    /**
     * 최신 수정일을 조회한다.
     * 수정일 범위
     * - 1. 고객사-프로바이더 매핑 수정 일시(삭제 포함)
     * - 2. 고객사-상품 매핑 수정 일시(삭제 포함)
     *
     * @param companyNo  고객회사 번호
     * @param providerNo 프로바이더 번호
     * @return LatestUpdatedRes
     */
    public LatestUpdatedRes getProviderLatestUpdated(Integer companyNo, Integer providerNo) {
        return customerCompanyProviderMappingRepository.findLatestUpdated(companyNo, providerNo);
    }

    /**
     * 최신 수정일을 조회한다.
     * 수정일 범위
     * - 1. 고객사-상품 매핑 수정 일시(삭제 포함)
     * - 2. 사용자 권한-상품 매핑 수정 일시(삭제 포함)
     *
     * @param companyNo  고객회사 번호
     * @param providerNo 프로바이더 번호
     * @param goodsNo    상품 번호
     * @return LatestUpdatedRes
     */
    public LatestUpdatedRes getGoodsLatestUpdated(Integer companyNo, Integer providerNo, Integer goodsNo) {
        return authManageGoodsMappingRepository.findLatestUpdated(companyNo, providerNo, goodsNo);
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
