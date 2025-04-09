package com.sk.domain.admin.service;

import org.springframework.data.domain.Pageable;

import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.RoleUtil;
import com.sk.domain.admin.dto.request.provider.ProviderCreateReq;
import com.sk.domain.admin.dto.request.provider.ProviderUpdateReq;
import com.sk.domain.admin.dto.response.provider.ProviderDetailRes;
import com.sk.domain.admin.dto.response.provider.ProviderLatestUpdatedRes;
import com.sk.domain.admin.dto.response.provider.ProviderNameRes;
import com.sk.domain.admin.dto.response.provider.ProviderRes;
import com.sk.domain.shared.entity.master.tmd.Provider;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.repository.master.tam.CustomerCompanyProviderMappingRepository;
import com.sk.domain.shared.repository.master.tmd.ProviderRepository;
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
     * @param user 사용자 정보
     * @param page 조회할 페이지 번호 (min 1)
     * @param size 조회할 요소 사이즈 (min 1)
     * @return PaginationRes<ProviderRes>
     */
    public PaginationRes<ProviderRes> getAllProviders(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);

        if (RoleUtil.isAdmin(user)) {
            return PaginationRes.toRes(providerRepository.findAllForAdmin(pageable));
        }

        if (RoleUtil.isOperator(user)) {
            return PaginationRes.toRes(providerRepository.findAllForOperator(user.getCustomerCompany().getCustomerCompanyNo(), pageable));
        }

        throw AppErrorException.of(ResultCode.Error.AUTH_REQUIRED, "user role not found");
    }

    /**
     * 데이타 프로바이더명 목록을 조회한다.
     * 
     * @param user 사용자 정보
     */
    public List<ProviderNameRes> getAllProviderNm(User user) {

        if (RoleUtil.isAdmin(user)) {
            return providerRepository.findProviderNmForAdmin();
        }

        if (RoleUtil.isOperator(user)) {
            return providerRepository.findProviderNmForOperator(user.getCustomerCompany().getCustomerCompanyNo());
        }

        throw AppErrorException.of(ResultCode.Error.AUTH_REQUIRED, "user role not found");
    }

    /**
     * 데이타 프로바이더를 등록한다.
     *
     * @param ProviderCreateReq 데이타 프로바이더 등록 데이터
     * @return ProviderCreateRes
     */
    @Transactional
    public void createProvider(ProviderCreateReq dto) {
        providerRepository.save(Provider.of(dto.providerNm(), dto.providerId(), dto.providerExpn(), dto.isUsed()));

        return;
    }

    /**
     * 데이타 프로바이더를 상세 조회한다.
     *
     * @param user 사용자 정보
     * @param providerNo 데이타 프로바이더 번호
     * @return ProviderDetailRes
     */
    public ProviderDetailRes getProvider(User user, int providerNo) {
        if (RoleUtil.isAdmin(user)) {
            //
        }

        if (RoleUtil.isOperator(user)) {
            customerCompanyProviderMappingRepository.findByCustomerCompanyAndId_ProviderNoAndIsDeletedFalse(user.getCustomerCompany(), providerNo)
                    .orElseThrow(
                            () -> AppErrorException.of(ResultCode.Error.REQ_METHOD_NOT_ALLOWED, String.format("provider no: %d", providerNo)));
        }

        return providerRepository.findProviderDetailByProviderNo(providerNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("provider no: %d", providerNo)));
    }

    /**
     * 데이타 프로바이더를 수정한다.
     *
     * @param ProviderUpdateReq 데이타 프로바이더 등록 데이터
     * @param providerNo 데이타 프로바이더 번호
     * @return ProviderUpdateRes
     */
    @Transactional
    public void updateProvider(int providerNo, ProviderUpdateReq dto) {

        Provider provider = providerRepository.findByProviderNoAndIsDeletedFalse(providerNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("provider no: %d", providerNo)));

        provider.update(dto.providerNm(), dto.providerExpn(), dto.isUsed());

        return;
    }

    /**
     * 데이타 프로바이더를 삭제한다.
     *
     * @param providerNo 데이타 프로바이더 번호
     * @return ProviderDeleteRes
     */
    @Transactional
    public void deleteProvider(int providerNo) {
        Provider provider = providerRepository.findByProviderNoAndIsDeletedFalse(providerNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("provider no: %d", providerNo)));

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
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("provider no: %d", providerNo)));
    }

}
