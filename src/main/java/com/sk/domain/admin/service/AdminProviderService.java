package com.sk.domain.admin.service;

import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.RoleUtil;
import com.sk.domain.admin.dto.request.provider.ProviderCreateReq;
import com.sk.domain.admin.dto.request.provider.ProviderUpdateReq;
import com.sk.domain.admin.dto.response.provider.ProviderCreateRes;
import com.sk.domain.admin.dto.response.provider.ProviderDetailRes;
import com.sk.domain.admin.dto.response.provider.ProviderLatestUpdatedRes;
import com.sk.domain.admin.dto.response.provider.ProviderNameRes;
import com.sk.domain.admin.dto.response.provider.ProviderRes;
import com.sk.domain.shared.entity.master.tmd.Provider;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.repository.master.tam.CustomerCompanyProviderMappingRepository;
import com.sk.domain.shared.repository.master.tmd.ProviderRepository;
import com.sk.domain.shared.service.permissions.AuthorizationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminProviderService {
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

        if (RoleUtil.isAdmin(role) || RoleUtil.isSubAdmin(role)) {
            // 관리자: 전체 프로바이더를 조회한다.
            return PaginationRes.toRes(providerRepository.findAllForAdmin(pageable));
        } else if (RoleUtil.isOperator(role) || RoleUtil.isClient(role)) {
            // 운영자: 고객사에 등록 된 프로바이더만 조회한다.
            return PaginationRes.toRes(providerRepository.findAllForOperator(companyNo, pageable));
        }

        return null;
    }

    /**
     * 데이타 프로바이더명 목록을 조회한다.
     * 
     * @param companyNo 사용자가 속한 고객사 번호
     * @param role 사용자 역할
     * @return List<ProviderNameRes>
     */
    public List<ProviderNameRes> getAllProviderNm(int companyNo, Role role) {

        if (RoleUtil.isAdmin(role)) {
            // 관리자: 전체 프로바이더명을 조회한다.
            return providerRepository.findProviderNmForAdmin();
        } else if (RoleUtil.isOperator(role) || RoleUtil.isClient(role)) {
            // 운영자 고객사에 등록 된 프로바이더명만 조회한다.
            return providerRepository.findProviderNmForOperator(companyNo);
        }

        return null;
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

        if (RoleUtil.isAdmin(role)) {
            // 관리자: 프로바이더를 조회한다.
            return providerRepository.findProviderDetailByProviderNo(providerNo)
                    .orElseThrow(
                            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "provider no: %d".formatted(providerNo)));
        } else if (RoleUtil.isOperator(role) || RoleUtil.isClient(role)) {
            // 운영자: 프로바이더 등록 유무 체크 후 조회한다.

            // 프로바이더 등록 유무 체크
            customerCompanyProviderMappingRepository
                    .findById_CustomerCompanyNoAndId_ProviderNoAndIsDeletedFalse(companyNo, providerNo)
                    .orElseThrow(
                            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "provider no: %d".formatted(providerNo)));

            return providerRepository.findProviderDetailByProviderNo(providerNo)
                    .orElseThrow(
                            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "provider no: %d".formatted(providerNo)));
        }

        throw AppErrorException.of(ResultCode.Error.AUTH_ACCESS_DENIED);
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

        return;
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

        return;
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
