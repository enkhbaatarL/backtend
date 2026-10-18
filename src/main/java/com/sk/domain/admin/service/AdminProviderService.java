package com.sk.domain.admin.service;

import java.sql.Date;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.CustomAuthorizationException;
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.DateUtil;
import com.sk.domain.admin.dto.request.provider.ProviderCreateReq;
import com.sk.domain.admin.dto.request.provider.ProviderUpdateReq;
import com.sk.domain.admin.dto.response.provider.ProviderCreateRes;
import com.sk.domain.admin.dto.response.provider.ProviderDetailRes;
import com.sk.domain.admin.dto.response.provider.ProviderLatestUpdatedRes;
import com.sk.domain.admin.dto.response.provider.ProviderNameRes;
import com.sk.domain.admin.dto.response.provider.ProviderRes;
import com.sk.domain.shared.entity.master.tcm.CommonCode;
import com.sk.domain.shared.entity.master.tcm.CommonCodeDetail;
import com.sk.domain.shared.entity.master.tmd.Provider;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.repository.master.tam.CustomerCompanyProviderMappingRepository;
import com.sk.domain.shared.repository.master.tcm.CommonCodeDetailRepository;
import com.sk.domain.shared.repository.master.tcm.CommonCodeRepository;
import com.sk.domain.shared.repository.master.tmd.ProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminProviderService {
    private final CommonCodeRepository commonCodeRepository;
    private final CommonCodeDetailRepository commonCodeDetailRepository;
    private final ProviderRepository providerRepository;
    private final CustomerCompanyProviderMappingRepository customerCompanyProviderMappingRepository;

    /**
     * 데이타 프로바이더 목록을 페이지네이션 처리하여 조회한다.
     *
     * @param companyNo 사용자가 속한 고객사 번호
     * @param role 사용자 역할
     * @param page 조회할 페이지 번호 (min 1)
     * @param size 조회할 요소 사이즈 (min 1)
     * @return PaginationRes<ProviderRes>
     */
    public PaginationRes<ProviderRes> getAllProviders(int companyNo, Role role, int page, int size) {

        Pageable pageable = PageRequest.of(page - 1, size);

        switch (role) {
            case ADMIN:
                return PaginationRes.toRes(providerRepository.findAllForAdmin(pageable));

            case OPERATOR:
                return PaginationRes.toRes(providerRepository.findAllForOperator(companyNo, pageable));

            case API_SUB_ADMIN:
                return PaginationRes.toRes(providerRepository.findAllForAdmin(pageable));

            case API_PLUS_OPERATOR:
                return PaginationRes.toRes(providerRepository.findAllForOperator(companyNo, pageable));

            default:
                throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS);
        }
    }

    /**
     * 데이타 프로바이더명 목록을 조회한다.
     *
     * @param companyNo 사용자가 속한 고객사 번호
     * @param role 사용자 역할
     * @return List<ProviderNameRes>
     */
    public List<ProviderNameRes> getAllProviderNm(int companyNo, Role role) {

        switch (role) {
            case ADMIN:
                return providerRepository.findProviderNmForAdmin();

            case OPERATOR:
                return providerRepository.findProviderNmForOperator(companyNo);

            case API_SUB_ADMIN:
                return providerRepository.findProviderNmForAdmin();

            case API_PLUS_OPERATOR:
                return providerRepository.findProviderNmForOperator(companyNo);

            default:
                throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS);
        }
    }

    /**
     * 데이타 프로바이더를 등록한다.
     *
     * @param ProviderCreateReq 데이타 프로바이더 데이터
     * @return ProviderCreateRes
     */
    @Transactional
    public ProviderCreateRes createProvider(ProviderCreateReq dto) {

        providerRepository.findByProviderIdAndIsDeletedFalse(dto.providerId())
                .ifPresent(provider -> {
                    throw AppErrorException.of(ResultCode.Error.RSC_ALREADY_EXISTS, "provider id: %s".formatted(dto.providerId()));
                });

        String highCodeGroupId = "gds_data_ctgr_cd";
        String highCodeId = "A_PROVIDERS";

        CommonCode commonCode = commonCodeRepository.findByCodeGroupId(highCodeGroupId)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, highCodeGroupId));

        Short codeRnk = (short) (commonCodeDetailRepository.findMaxCodeRnkByHighCodeId(commonCode.getCodeGroupId(), highCodeId) + 1);

        CommonCodeDetail commonCodeDetail = CommonCodeDetail.of(commonCode.getCodeGroupId(), "A_".concat(dto.providerId()), dto.providerNm(),
                codeRnk != null ? codeRnk : 1,
                dto.providerExpn(),
                DateUtil.currentYmd(), DateUtil.endYmd(), dto.isUsed(), commonCode.getCodeGroupId(), highCodeId, commonCode);
        commonCodeDetailRepository.save(commonCodeDetail);

        return ProviderCreateRes
                .of(providerRepository.save(Provider.of(dto.providerNm(), dto.providerId(), dto.providerExpn(), dto.isUsed())));
    }

    /**
     * 데이타 프로바이더를 상세 조회한다.
     *
     * @param companyNo 사용자가 속한 고객사 번호
     * @param role 사용자 역할
     * @param providerNo 데이타 프로바이더 번호
     * @return ProviderDetailRes
     */
    public ProviderDetailRes getProvider(int companyNo, Role role, int providerNo) {

        switch (role) {
            case ADMIN:
                return providerRepository.findProviderDetailByProviderNo(providerNo)
                        .orElseThrow(
                                () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "provider no: %d".formatted(providerNo)));

            case OPERATOR:
                customerCompanyProviderMappingRepository
                        .findById_CustomerCompanyNoAndId_ProviderNoAndIsDeletedFalse(companyNo, providerNo)
                        .orElseThrow(
                                () -> AppErrorException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "provider no: %d".formatted(providerNo)));

                return providerRepository.findProviderDetailByProviderNo(providerNo)
                        .orElseThrow(
                                () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "provider no: %d".formatted(providerNo)));

            case API_SUB_ADMIN:
                return providerRepository.findProviderDetailByProviderNo(providerNo)
                        .orElseThrow(
                                () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "provider no: %d".formatted(providerNo)));

            case API_PLUS_OPERATOR:
                customerCompanyProviderMappingRepository
                        .findById_CustomerCompanyNoAndId_ProviderNoAndIsDeletedFalse(companyNo, providerNo)
                        .orElseThrow(
                                () -> AppErrorException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "provider no: %d".formatted(providerNo)));

                return providerRepository.findProviderDetailByProviderNo(providerNo)
                        .orElseThrow(
                                () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "provider no: %d".formatted(providerNo)));

            default:
                throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS);
        }
    }

    /**
     * 데이타 프로바이더를 수정한다.
     *
     * @param providerNo 데이타 프로바이더 번호
     * @param ProviderUpdateReq 데이타 프로바이더 데이터
     * @return
     */
    @Transactional
    public void updateProvider(int providerNo, ProviderUpdateReq dto) {

        Provider provider = providerRepository.findByProviderNoAndIsDeletedFalse(providerNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "provider no: %d".formatted(providerNo)));

        provider.update(dto.providerNm(), dto.providerExpn(), dto.isUsed());

    }

    /**
     * 데이타 프로바이더를 삭제한다.
     *
     * @param providerNo 데이타 프로바이더 번호
     * @return
     */
    @Transactional
    public void deleteProvider(int providerNo) {
        Provider provider = providerRepository.findByProviderNoAndIsDeletedFalse(providerNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "provider no: %d".formatted(providerNo)));

        provider.softDelete();

    }

    /**
     * 해당 프로바이더의 최신 수정일 노출
     * 프로바이더 정보 수정 업데이트 범위 : 프로바이더 정 보 등록/수정, 상품 등록/수정/삭제
     *
     * @param providerNo 데이타 프로바이더 번호
     * @return ProviderLatestUpdatedRes
     */
    public ProviderLatestUpdatedRes getProviderLatestUpdated(int providerNo) {
        return providerRepository.findProviderLatestUpdatedByProviderNo(providerNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "provider no: %d".formatted(providerNo)));
    }
}
