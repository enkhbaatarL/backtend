package com.sk.domain.admin.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.common.model.ValidationResult;
import com.sk.common.utils.AESUtil;
import com.sk.domain.admin.dto.request.client.ClientAccessInfoReq;
import com.sk.domain.admin.dto.response.client.ClientAccessInfoRes;
import com.sk.domain.admin.dto.response.client.ClientAccessSecretRes;
import com.sk.domain.admin.validator.ClientAccessInfoValidator;
import com.sk.domain.admin.validator.CompanyValidator;
import com.sk.domain.shared.entity.master.tif.ClientAccessInfo;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.repository.master.tif.ClientAccessInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminSystemService {

    @Value("${aes.secret-key}")
    private String secretKey;

    private final CompanyValidator companyValidator;
    private final ClientAccessInfoRepository clientAccessInfoRepository;
    private final ClientAccessInfoValidator clientAccessInfoValidator;

    /**
     * 클라이언트 접근권한 목록을 조회합니다.
     * - 정렬 기준: 생성일자(createDt) 내림차순으로 정렬됩니다.
     *
     * @param page 조회할 페이지 번호 (최소값: 1)
     * @param size 조회할 요소 개수 (최소값: 1)
     * @return 클라이언트 접근권한 정보 목록을 포함한 PaginationRes
     */
    public PaginationRes<ClientAccessInfoRes> getClientAccessInfos(int page, int size) {
        return PaginationRes.toRes(clientAccessInfoRepository.findClientInfoByConditions(page, size));
    }

    /**
     * 새 클라이언트 접근권한을 등록합니다.
     *
     * @param dto 클라이언트 접근권한 정보를 포함하는 요청 데이터 (ClientAccessInfoReq.Insert)
     * @return 등록된 클라이언트 접근권한 정보 (ClientAccessInfoRes)
     */
    @Transactional
    public ClientAccessInfoRes postClientAccessInfo(ClientAccessInfoReq.Insert dto) {

        CustomerCompany customerCompany = companyValidator.ensureCompanyExistsByCompanyNo(dto.customerCompanyNo()).getOrThrow();

        clientAccessInfoRepository.findByClientCertIdAndIsDeletedFalse(dto.clientCertId())
            .ifPresent(clientAccessInfo -> {
                throw AppErrorException.of(ResultCode.Error.RSC_ALREADY_EXISTS, String.format("ClientCertId [%s] already exists.", dto.clientCertId()));
            });

        String clientSecret = AESUtil.generateSecret();
        String encryptedClientSecret = AESUtil.encrypt(clientSecret, secretKey);

        ClientAccessInfo clientAccessInfo = clientAccessInfoRepository.save(ClientAccessInfo.of(dto, encryptedClientSecret, customerCompany));

        return ClientAccessInfoRes.of(clientAccessInfo, customerCompany, secretKey);

    }

    /**
     * 클라이언트 Secret-Key를 재발급합니다.
     *
     * @param clientAccessRegNo 클라이언트 접속 등록 번호 (고유 식별자)
     * @return 재발급된 Secret-Key 및 클라이언트 접근권한 정보 (ClientAccessInfoRes)
     */
    @Transactional
    public ClientAccessInfoRes patchClientAccessInfo(Integer clientAccessRegNo) {

        ValidationResult<ClientAccessInfo> clientAccessInfoValid = clientAccessInfoValidator.ensureExists(clientAccessRegNo);
        ClientAccessInfo clientAccessInfo = clientAccessInfoValid.getOrThrow();

        String clientSecret = AESUtil.generateSecret();
        String encryptedClientSecret = AESUtil.encrypt(clientSecret, secretKey);

        clientAccessInfo.update(encryptedClientSecret);

        return ClientAccessInfoRes.of(clientAccessInfo, clientAccessInfo.getCustomerCompany(), secretKey);

    }


    /**
     * 클라이언트 접근권한 정보를 삭제(소프트 삭제)합니다.
     *
     * @param clientAccessRegNo 클라이언트 접속 등록 번호
     * @return 삭제된 상태의 클라이언트 접근권한 정보 (ClientAccessInfoRes)
     */
    @Transactional
    public ClientAccessInfoRes deleteClientAccessInfo(Integer clientAccessRegNo) {

        ValidationResult<ClientAccessInfo> clientAccessInfoValid = clientAccessInfoValidator.ensureExists(clientAccessRegNo);
        ClientAccessInfo clientAccessInfo = clientAccessInfoValid.getOrThrow();

        clientAccessInfo.softDelete();

        return ClientAccessInfoRes.of(clientAccessInfo, clientAccessInfo.getCustomerCompany(), secretKey);

    }

    /**
     * 복호화된 클라이언트 Secret-Key 값을 조회합니다.
     *
     * @param clientAccessRegNo 클라이언트 접속 등록 번호 (고유 식별자)
     * @return 복호화된 Secret-Key를 포함한 응답 객체 (ClientAccessSecretRes)
     */
    public ClientAccessSecretRes getClientAccessInfoSecret(Integer clientAccessRegNo) {

        ValidationResult<ClientAccessInfo> clientAccessInfoValid = clientAccessInfoValidator.ensureExists(clientAccessRegNo);
        ClientAccessInfo clientAccessInfo = clientAccessInfoValid.getOrThrow();

        return ClientAccessSecretRes.of(AESUtil.decrypt(clientAccessInfo.getClientSecretKey(), secretKey));

    }

}
