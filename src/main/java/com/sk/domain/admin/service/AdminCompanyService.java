package com.sk.domain.admin.service;

import com.sk.common.model.PaginationRes;
import com.sk.common.model.ValidationResult;
import com.sk.common.utils.SortUtil;
import com.sk.domain.admin.dto.request.company.AuthGoodsMapReq;
import com.sk.domain.admin.dto.request.company.CompanyGoodsMapReq;
import com.sk.domain.admin.dto.request.company.CompanyProviderMapReq;
import com.sk.domain.admin.dto.request.company.CompanyReq;
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
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.GoodsAuthCd;
import com.sk.domain.shared.enums.GoodsAuthManageSupportCd;
import com.sk.domain.shared.repository.master.tam.AuthManageGoodsMappingRepository;
import com.sk.domain.shared.repository.master.tam.CustomerCompanyGoodsMappingRepository;
import com.sk.domain.shared.repository.master.tam.CustomerCompanyProviderMappingRepository;
import com.sk.domain.shared.repository.master.tif.CompanyInfoRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.repository.master.tmd.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
    private final UserRepository userRepository;

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
    public CompanyProviderMapRes postProviderMappedToCompany(Integer companyNo, CompanyProviderMapReq companyProviderMapReq) {

        ValidationResult<CustomerCompany> companyValid = companyValidator.ensureCompanyExistsByCompanyNo(companyNo);
        CustomerCompany company = companyValid.getOrThrow();

        ValidationResult<Provider> providerValid = companyValidator.ensureProviderExistsByProviderNo(companyProviderMapReq.providerNo());
        Provider provider = providerValid.getOrThrow();

        companyValidator.ensureCcpmNotExistsByCompanyAndProvider(company, provider);

        return CompanyProviderMapRes.of(customerCompanyProviderMappingRepository.save(CustomerCompanyProviderMapping.of(company, provider)));

    }

    /**
     * 고객사와 매핑되어 있는 프로바이더를 상세 조회한다.
     *
     * @param companyNo  고객회사 번호
     * @param providerNo 프로바이더 번호
     * @return ProviderMappingDetailRes
     */
    public CompanyProviderMapDetailRes getProviderDetail(Integer companyNo, Integer providerNo) {

        ValidationResult<CustomerCompanyProviderMapping> ccpValid = companyValidator.ensureCcpmExistsByCompanyNoAndProviderNo(companyNo, providerNo);
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
    public PaginationRes<GoodsWithDataCntRes> getGoodsMappedToCompany(int page, int size, Integer companyNo, Integer providerNo) {
        return PaginationRes.toRes(customerCompanyGoodsMappingRepository.findGoodsMappingsByConditions(page, size, companyNo, providerNo));
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
    public CompanyGoodsMapRes postGoodsMappedToCompany(Integer companyNo, Integer providerNo, CompanyGoodsMapReq companyGoodsMapReq) {

        ValidationResult<CustomerCompany> companyValid = companyValidator.ensureCompanyExistsByCompanyNo(companyNo);
        CustomerCompany company = companyValid.getOrThrow();

        ValidationResult<CustomerCompanyProviderMapping> ccpValid = companyValidator.ensureCcpmExistsByCompanyNoAndProviderNo(companyNo, providerNo);
        CustomerCompanyProviderMapping ccp = ccpValid.getOrThrow();

        ValidationResult<CustomerCompanyGoodsMapping> ccgValid = companyValidator.ensureCcgmNotExistsByCompanyNoAndGoodsNo(companyNo,
            companyGoodsMapReq.goodsNo());
        CustomerCompanyGoodsMapping ccg = ccgValid.getOrThrow();

        ValidationResult<Goods> goodsValid = companyValidator.ensureGoodsExistsByGoodsNo(providerNo, companyGoodsMapReq.goodsNo());
        Goods goods = goodsValid.getOrThrow();

        return CompanyGoodsMapRes.of(customerCompanyGoodsMappingRepository.save(CustomerCompanyGoodsMapping.of(company, goods, companyGoodsMapReq.goodsAuthManageCd())));
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

        ValidationResult<CustomerCompanyProviderMapping> ccpResult = companyValidator.ensureCcpmExistsByCompanyNoAndProviderNo(companyNo, providerNo);
        ccpResult.validOrThrow();

        ValidationResult<CustomerCompanyGoodsMapping> ccgResult = companyValidator.ensureCcgmExistsByCompanyNoAndGoodsNo(companyNo, goodsNo);
        CustomerCompanyGoodsMapping ccg = ccgResult.getOrThrow();

        if (GoodsAuthManageSupportCd.COMPANY == ccg.getId().getGoodsAuthManageSupportCd()) {
            List<AuthManageGoodsMapping> companyAuth = authManageGoodsMappingRepository.findByCompanyNoAndGoodsNoAndGoodsAuthCd(companyNo, goodsNo,
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
    public PaginationRes<AuthGoodsMapRes> getAuthGoods(int page, int size, Integer companyNo, Integer providerNo, Integer goodsNo) {

        ValidationResult<CustomerCompanyProviderMapping> ccpResult = companyValidator.ensureCcpmExistsByCompanyNoAndProviderNo(companyNo, providerNo);
        ccpResult.validOrThrow();

        return PaginationRes.toRes(authManageGoodsMappingRepository.findByAuthGoodsByConditions(page, size, companyNo, goodsNo));

    }

//    /**
//     * 고객사와 매핑되어 있는 상품에 대한 사용자 권한을 설정한다.
//     * [수정 대상]
//     * - 고객사-상품 매핑
//     * 1. 사용 여부
//     * 2. 권한 유형(개별 권한, 회사 권한)
//     * - 권한-상품 매핑
//     * 1. 사용자 별 상세 권한 유형(조회 권한, 다운로드 권한)
//     * 2. 권한 적용 기간
//     *
//     * @param companyNo       고객회사 번호
//     * @param providerNo      프로바이더 번호
//     * @param goodsNo         상품 번호
//     * @param authGoodsMapReq 권한상품DTO
//     * @return AuthGoodsMappingRes
//     */
//    @Transactional
//    public AuthGoodsMapSettingRes patchGoodsAuth(Integer companyNo, Integer providerNo, Integer goodsNo, AuthGoodsMapReq authGoodsMapReq) {
//
//        ValidationResult<CustomerCompanyProviderMapping> ccpResult = companyValidator.ensureCcpmExistsByCompanyNoAndProviderNo(companyNo, providerNo);
//        ccpResult.validOrThrow();
//
//        ValidationResult<CustomerCompanyGoodsMapping> ccgResult = companyValidator.ensureCcgmExistsByCompanyNoAndGoodsNo(companyNo, goodsNo);
//        CustomerCompanyGoodsMapping ccg = ccgResult.getOrThrow();
//
//        List<AuthManageGoodsMapping> existingAuthMappings = authManageGoodsMappingRepository.findByCustomerCompanyGoodsMappingAndIsDeletedFalse(ccg);
//
//        // 1. 고객사-상품 매핑 수정
//        if (authGoodsMapReq.goodsAuthManageCd() == ccg.getId().getGoodsAuthManageSupportCd()) {
//            ccg.update(authGoodsMapReq);
//
//            // 2. 권한-상품 매핑 수정
//            if (GoodsAuthManageSupportCd.COMPANY == authGoodsMapReq.goodsAuthManageCd()) {
//                // 회사 단위 권한 관리 처리
//                handleCompanyLevelPermissions(ccg, existingAuthMappings, authGoodsMapReq, false);
//            } else if (GoodsAuthManageSupportCd.INDIVIDUAL == authGoodsMapReq.goodsAuthManageCd()) {
//                // 개인 단위 권한 관리 처리
//                handleIndividualLevelPermissions(ccg, existingAuthMappings, authGoodsMapReq, false);
//            }
//
//
//        } else {
//            ccg.softDelete();
//            ccg = customerCompanyGoodsMappingRepository.save(CustomerCompanyGoodsMapping.of(ccg.getCustomerCompany(), ccg.getGoods(), authGoodsMapReq.goodsAuthManageCd(), authGoodsMapReq.isUsed()));
//
//            // 2. 권한-상품 매핑 수정
//            if (GoodsAuthManageSupportCd.COMPANY == authGoodsMapReq.goodsAuthManageCd()) {
//                // 회사 단위 권한 관리 처리
//                handleCompanyLevelPermissions(ccg, existingAuthMappings, authGoodsMapReq, true);
//            } else if (GoodsAuthManageSupportCd.INDIVIDUAL == authGoodsMapReq.goodsAuthManageCd()) {
//                // 개인 단위 권한 관리 처리
//                handleIndividualLevelPermissions(ccg, existingAuthMappings, authGoodsMapReq, true);
//            }
//
//        }
//
//        return AuthGoodsMapSettingRes.of(authGoodsMapReq);
//
//    }
//
//    private void handleCompanyLevelPermissions(CustomerCompanyGoodsMapping ccg, List<AuthManageGoodsMapping> existingAuthMappings, AuthGoodsMapReq authGoodsMapReq, boolean isChangedSupportCd) {
//
//        // 개인 -> 회사 권한 변경 시
//        if (isChangedSupportCd) {
//            // 기존에 가지고 있던 개인 권한 모두 deleted
//            existingAuthMappings.forEach(BaseEntity::softDelete);
//        }
//
//        // 회사 단위 설정이므로 사용자 번호 없이 권한 설정
//        AuthGoodsMapReq.CompanySetting companySetting = authGoodsMapReq.companySetting();
//
//        // companySetting이 null인 경우 처리
//        if (companySetting == null) {
//            return; // 권한 설정 없이 종료
//        }
//
//        Map<GoodsAuthCd, AuthManageGoodsMapping> previousAuthMap = existingAuthMappings.stream()
//            .filter(s -> !s.getIsDeleted())
//            .collect(Collectors.toMap(AuthManageGoodsMapping::getGoodsAuthCd, Function.identity()));
//
//        // 조회(VIEW) 권한 생성
//        // 기존에 조회 권한이 없고, 회사 설정에서 조회 가능 설정을 한 경우
//        if (ObjectUtils.isEmpty(previousAuthMap.get(GoodsAuthCd.VIEW)) && companySetting.isViewable()) {
//            AuthManageGoodsMapping save = authManageGoodsMappingRepository.save(AuthManageGoodsMapping.of(companySetting, GoodsAuthCd.VIEW, ccg));
//            previousAuthMap.put(GoodsAuthCd.VIEW, save);
//        }
//
//        // 다운로드(DOWNLOAD) 권한 생성
//        // 기존에 다운로드 권한이 없고, 회사 설정에서 다운로드 가능 설정을 한 경우
//        if (ObjectUtils.isEmpty(previousAuthMap.get(GoodsAuthCd.DOWNLOAD)) && companySetting.isDownloadable()) {
//            AuthManageGoodsMapping save = authManageGoodsMappingRepository.save(AuthManageGoodsMapping.of(companySetting, GoodsAuthCd.DOWNLOAD, ccg));
//            previousAuthMap.put(GoodsAuthCd.DOWNLOAD, save);
//        }
//
//        // 기존 매핑이 있고 사용 기간이 변경된 경우 업데이트
//        boolean beginDateChanged = !existingAuthMappings.isEmpty() && !existingAuthMappings.get(0).getUseBeginDate().equals(authGoodsMapReq.companySetting().usageBeginDate());
//        boolean endDateChanged = !existingAuthMappings.isEmpty() && !existingAuthMappings.get(0).getUseEndDate().equals(authGoodsMapReq.companySetting().usageEndDate());
//        if (beginDateChanged || endDateChanged) {
//            existingAuthMappings.forEach(s -> s.update(authGoodsMapReq.companySetting()));
//        }
//
//        // 삭제
//        if (ObjectUtils.isNotEmpty(previousAuthMap.get(GoodsAuthCd.VIEW)) && !companySetting.isViewable()) {
//            previousAuthMap.get(GoodsAuthCd.VIEW).softDelete();
//        }
//
//        // 삭제
//        if (ObjectUtils.isNotEmpty(previousAuthMap.get(GoodsAuthCd.DOWNLOAD)) && !companySetting.isDownloadable()) {
//            previousAuthMap.get(GoodsAuthCd.DOWNLOAD).softDelete();
//        }
//
//    }
//
//    /**
//     * 개인 단위 권한 관리 처리
//     */
//    private void handleIndividualLevelPermissions(CustomerCompanyGoodsMapping ccg, List<AuthManageGoodsMapping> existingAuthMappings, AuthGoodsMapReq authGoodsMapReq, boolean isChangedSupportCd) {
//
//        List<AuthGoodsMapReq.IndividualSetting> individualSettings = authGoodsMapReq.IndividualSettings();
//
//        // 회사 -> 개인 권한 변경 시
//        if (isChangedSupportCd) {
//            // 기존에 가지고 있던 회사 권한 모두 deleted
//            existingAuthMappings.forEach(BaseEntity::softDelete);
//        }
//
//        if (individualSettings == null || individualSettings.isEmpty()) {
//            return;
//        }
//
//        List<Integer> ids = authGoodsMapReq.IndividualSettings().stream().map(AuthGoodsMapReq.IndividualSetting::userNo).toList();
//
//        // 고객사 사용자 전체 정보
//        List<User> users = userRepository.findByUserNoInAndIsDeletedFalse(ids);
//        Map<Integer, User> userMap = users.stream().collect(Collectors.toMap(User::getUserNo, Function.identity()));
//
//        // 기존 권한 매핑을 사용자 번호로 그룹화하여 빠른 검색을 위한 맵 생성
//        Map<Integer, List<AuthManageGoodsMapping>> previousAuthUserMap = existingAuthMappings.stream()
//            .filter(mapping -> mapping.getUser() != null)
//            .collect(Collectors.groupingBy(mapping -> mapping.getUser().getUserNo()));
//
//        // 요청에 포함된 각 사용자에 대한 권한 처리
//        for (AuthGoodsMapReq.IndividualSetting individualSetting : individualSettings) {
//
//            // 사용자 정보 조회
//            User user = userMap.get(individualSetting.userNo());
//
//            // 사용자가 존재하지 않는 경우
//            if (user == null) {
//                continue;
//            }
//
//            // 해당 사용자의 기존 권한 매핑 목록
//            List<AuthManageGoodsMapping> previousAuthMappings = previousAuthUserMap.getOrDefault(individualSetting.userNo(), Collections.emptyList());
//
//            Map<GoodsAuthCd, AuthManageGoodsMapping> previousAuthMap = previousAuthMappings.stream()
//                .filter(s -> !s.getIsDeleted())
//                .collect(Collectors.toMap(AuthManageGoodsMapping::getGoodsAuthCd, Function.identity()));
//
//            // 조회(VIEW) 권한 생성
//            // 기존에 조회 권한이 없고, 회사 설정에서 조회 가능 설정을 한 경우
//            if (ObjectUtils.isEmpty(previousAuthMap.get(GoodsAuthCd.VIEW)) && individualSetting.isViewable()) {
//                AuthManageGoodsMapping save = authManageGoodsMappingRepository.save(AuthManageGoodsMapping.of(user, individualSetting, GoodsAuthCd.VIEW, ccg));
//                previousAuthMap.put(GoodsAuthCd.VIEW, save);
//            }
//
//            // 다운로드(DOWNLOAD) 권한 생성
//            // 기존에 다운로드 권한이 없고, 회사 설정에서 다운로드 가능 설정을 한 경우
//            if (ObjectUtils.isEmpty(previousAuthMap.get(GoodsAuthCd.DOWNLOAD)) && individualSetting.isDownloadable()) {
//                AuthManageGoodsMapping save = authManageGoodsMappingRepository.save(AuthManageGoodsMapping.of(user, individualSetting, GoodsAuthCd.DOWNLOAD, ccg));
//                previousAuthMap.put(GoodsAuthCd.DOWNLOAD, save);
//            }
//
//            // 기존 매핑이 있고 사용 기간이 변경된 경우 업데이트
//            boolean beginDateChanged = !existingAuthMappings.isEmpty() && !existingAuthMappings.get(0).getUseBeginDate().equals(individualSetting.usageBeginDate());
//            boolean endDateChanged = !existingAuthMappings.isEmpty() && !existingAuthMappings.get(0).getUseEndDate().equals(individualSetting.usageEndDate());
//            if (beginDateChanged || endDateChanged) {
//                existingAuthMappings.forEach(s -> s.update(individualSetting));
//            }
//
//            // 삭제
//            if (ObjectUtils.isNotEmpty(previousAuthMap.get(GoodsAuthCd.VIEW)) && !individualSetting.isViewable()) {
//                previousAuthMap.get(GoodsAuthCd.VIEW).softDelete();
//            }
//
//            // 삭제
//            if (ObjectUtils.isNotEmpty(previousAuthMap.get(GoodsAuthCd.DOWNLOAD)) && !individualSetting.isDownloadable()) {
//                previousAuthMap.get(GoodsAuthCd.DOWNLOAD).softDelete();
//            }
//
//        }
//
//    }

    /**
     * 고객사와 매핑되어 있는 상품에 대한 사용자 권한을 설정한다.
     */
    @Transactional
    public AuthGoodsMapSettingRes patchGoodsAuth(Integer companyNo, Integer providerNo, Integer goodsNo, AuthGoodsMapReq authGoodsMapReq) {
        // 유효성 검증
        ValidationResult<CustomerCompanyProviderMapping> ccpResult = companyValidator.ensureCcpmExistsByCompanyNoAndProviderNo(companyNo, providerNo);
        ccpResult.validOrThrow();

        ValidationResult<CustomerCompanyGoodsMapping> ccgResult = companyValidator.ensureCcgmExistsByCompanyNoAndGoodsNo(companyNo, goodsNo);
        CustomerCompanyGoodsMapping ccg = ccgResult.getOrThrow();

        // 기존 권한 매핑 조회
        List<AuthManageGoodsMapping> existingAuthMappings = authManageGoodsMappingRepository.findByCustomerCompanyGoodsMappingAndIsDeletedFalse(ccg);

        // 권한 구분 코드 변경 여부 확인
        boolean hasChangedSupportCd = authGoodsMapReq.goodsAuthManageCd() != ccg.getId().getGoodsAuthManageSupportCd();

        // 고객사-상품 매핑 처리
        CustomerCompanyGoodsMapping targetMapping = updateCompanyGoodsMapping(ccg, authGoodsMapReq, hasChangedSupportCd);

        // 권한 매핑 처리
        processAuthMappings(targetMapping, existingAuthMappings, authGoodsMapReq, hasChangedSupportCd);

        return AuthGoodsMapSettingRes.of(authGoodsMapReq);
    }

    /**
     * 고객사-상품 매핑 업데이트
     */
    private CustomerCompanyGoodsMapping updateCompanyGoodsMapping(CustomerCompanyGoodsMapping ccg, AuthGoodsMapReq req, boolean hasChangedSupportCd) {
        if (hasChangedSupportCd) {
            // 권한 구분 코드가 변경된 경우 기존 매핑을 삭제하고 새로 생성
            ccg.softDelete();
            return customerCompanyGoodsMappingRepository.save(CustomerCompanyGoodsMapping.of(ccg.getCustomerCompany(), ccg.getGoods(), req.goodsAuthManageCd(), req.isUsed()));
        } else {
            // 권한 구분 코드가 동일한 경우 기존 매핑 업데이트
            ccg.update(req);
            return ccg;
        }
    }

    /**
     * 권한 매핑 처리
     */
    private void processAuthMappings(CustomerCompanyGoodsMapping ccg, List<AuthManageGoodsMapping> existingMappings, AuthGoodsMapReq req, boolean hasChangedSupportCd) {
        // 권한 처리 전략 선택
        AuthMappingStrategy strategy = getAuthMappingStrategy(req.goodsAuthManageCd());

        // 권한 처리 전략 실행
        strategy.processAuthMappings(ccg, existingMappings, req, hasChangedSupportCd);
    }

    /**
     * 권한 처리 전략 인터페이스
     */
    private interface AuthMappingStrategy {
        void processAuthMappings(CustomerCompanyGoodsMapping ccg, List<AuthManageGoodsMapping> existingMappings, AuthGoodsMapReq req, boolean hasChangedSupportCd);
    }

    /**
     * 권한 처리 전략 팩토리
     */
    private AuthMappingStrategy getAuthMappingStrategy(GoodsAuthManageSupportCd supportCd) {
        if (GoodsAuthManageSupportCd.COMPANY == supportCd) {
            return this::handleCompanyLevelPermissions;
        } else if (GoodsAuthManageSupportCd.INDIVIDUAL == supportCd) {
            return this::handleIndividualLevelPermissions;
        }
        throw new IllegalArgumentException("지원하지 않는 권한 구분 코드입니다: " + supportCd);
    }

    /**
     * 회사 단위 권한 처리
     */
    private void handleCompanyLevelPermissions(CustomerCompanyGoodsMapping ccg, List<AuthManageGoodsMapping> existingMappings, AuthGoodsMapReq req, boolean hasChangedSupportCd) {
        // 권한 구분 코드 변경 시 기존 권한 소프트 삭제
        if (hasChangedSupportCd) {
            existingMappings.forEach(BaseEntity::softDelete);
        }

        AuthGoodsMapReq.CompanySetting companySetting = req.companySetting();
        if (companySetting == null) {
            return;
        }

        // 현재 활성 권한 맵 구성
        Map<GoodsAuthCd, AuthManageGoodsMapping> currentAuthMap = existingMappings.stream()
            .filter(s -> !s.getIsDeleted())
            .collect(Collectors.toMap(AuthManageGoodsMapping::getGoodsAuthCd, Function.identity()));

        // 권한 처리 (조회/다운로드)
        processPermission(currentAuthMap, GoodsAuthCd.VIEW, companySetting.isViewable(),
            () -> AuthManageGoodsMapping.of(companySetting, GoodsAuthCd.VIEW, ccg), ccg);

        processPermission(currentAuthMap, GoodsAuthCd.DOWNLOAD, companySetting.isDownloadable(),
            () -> AuthManageGoodsMapping.of(companySetting, GoodsAuthCd.DOWNLOAD, ccg), ccg);

        // 기간 변경 시 업데이트
        updatePermissionPeriodIfChanged(existingMappings, companySetting);
    }

    /**
     * 개인 단위 권한 처리
     */
    private void handleIndividualLevelPermissions(CustomerCompanyGoodsMapping ccg, List<AuthManageGoodsMapping> existingMappings, AuthGoodsMapReq req, boolean hasChangedSupportCd) {
        // 권한 구분 코드 변경 시 기존 권한 소프트 삭제
        if (hasChangedSupportCd) {
            existingMappings.forEach(BaseEntity::softDelete);
        }

        List<AuthGoodsMapReq.IndividualSetting> individualSettings = req.IndividualSettings();
        if (individualSettings == null || individualSettings.isEmpty()) {
            return;
        }

        // 사용자 정보 조회 및 매핑
        List<Integer> userIds = individualSettings.stream()
            .map(AuthGoodsMapReq.IndividualSetting::userNo)
            .collect(Collectors.toList());

        List<User> users = userRepository.findByUserNoInAndIsDeletedFalse(userIds);
        Map<Integer, User> userMap = users.stream()
            .collect(Collectors.toMap(User::getUserNo, Function.identity()));

        // 기존 권한 맵 구성
        Map<Integer, List<AuthManageGoodsMapping>> userAuthMap = existingMappings.stream()
            .filter(mapping -> mapping.getUser() != null)
            .collect(Collectors.groupingBy(mapping -> mapping.getUser().getUserNo()));

        // 개별 사용자 권한 처리
        individualSettings.forEach(setting -> {
            User user = userMap.get(setting.userNo());
            if (user == null) return;

            processIndividualUserPermissions(ccg, userAuthMap, user, setting);
        });
    }

    /**
     * 개별 사용자 권한 처리
     */
    private void processIndividualUserPermissions(CustomerCompanyGoodsMapping ccg, Map<Integer, List<AuthManageGoodsMapping>> userAuthMap,
                                                  User user, AuthGoodsMapReq.IndividualSetting setting) {
        // 현재 사용자의 권한 매핑
        List<AuthManageGoodsMapping> userMappings = userAuthMap.getOrDefault(user.getUserNo(), Collections.emptyList());

        // 현재 활성 권한 맵 구성
        Map<GoodsAuthCd, AuthManageGoodsMapping> currentAuthMap = userMappings.stream()
            .filter(s -> !s.getIsDeleted())
            .collect(Collectors.toMap(AuthManageGoodsMapping::getGoodsAuthCd, Function.identity()));

        // 권한 처리 (조회/다운로드)
        processPermission(currentAuthMap, GoodsAuthCd.VIEW, setting.isViewable(), () -> AuthManageGoodsMapping.of(user, setting, GoodsAuthCd.VIEW, ccg), ccg);

        processPermission(currentAuthMap, GoodsAuthCd.DOWNLOAD, setting.isDownloadable(), () -> AuthManageGoodsMapping.of(user, setting, GoodsAuthCd.DOWNLOAD, ccg), ccg);

        // 기간 변경 시 업데이트
        updatePermissionPeriodIfChanged(userMappings, setting);
    }

    /**
     * 특정 권한 처리 (생성/삭제)
     */
    private void processPermission(Map<GoodsAuthCd, AuthManageGoodsMapping> currentAuthMap, GoodsAuthCd authCd, boolean isEnabled, Supplier<AuthManageGoodsMapping> mappingCreator, CustomerCompanyGoodsMapping ccg) {
        // 권한이 필요하고 없는 경우 생성
        if (isEnabled && !currentAuthMap.containsKey(authCd)) {
            AuthManageGoodsMapping newMapping = mappingCreator.get();
            AuthManageGoodsMapping savedMapping = authManageGoodsMappingRepository.save(newMapping);
            currentAuthMap.put(authCd, savedMapping);
        }
        // 권한이 불필요하고 있는 경우 삭제
        else if (!isEnabled && currentAuthMap.containsKey(authCd)) {
            currentAuthMap.get(authCd).softDelete();
        }
    }

    /**
     * 권한 기간 변경 시 업데이트
     */
    private void updatePermissionPeriodIfChanged(List<AuthManageGoodsMapping> mappings, AuthGoodsMapReq.AuthSetting setting) {
        if (mappings.isEmpty()) return;

        String beginDate = setting.usageBeginDate();
        String endDate = setting.usageEndDate();

        boolean beginDateChanged = !mappings.get(0).getUseBeginDate().equals(beginDate);
        boolean endDateChanged = !mappings.get(0).getUseEndDate().equals(endDate);

        if (beginDateChanged || endDateChanged) {
            mappings.forEach(s -> s.update(setting));
        }
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

}
