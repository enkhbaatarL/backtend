package com.sk.domain.admin.service;

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
     * 클라이언트 접근권한 목록을 조회한다.
     * - Sort : createDt DESC
     *
     * @param page          조회할 페이지 번호 (min 1)
     * @param size          조회할 요소 사이즈 (min 1)
     * @return List<ClientAccessInfoRes>
     */
    public PaginationRes<ClientAccessInfoRes> getClientAccessInfos(int page, int size) {
        return PaginationRes.toRes(clientAccessInfoRepository.findClientInfoByConditions(page, size));
    }

    /**
     * 클라이언트 접근권한을 등록합니다.
     *
     * @param dto ClientAccessInfoReq.Insert
     * @return ClientAccessInfoRes
     */
    @Transactional
    public ClientAccessInfoRes postClientAccessInfo(ClientAccessInfoReq.Insert dto) {

        ValidationResult<CustomerCompany> companyValid = companyValidator.ensureCompanyExistsByCompanyNo(dto.customerCompanyNo());
        CustomerCompany customerCompany = companyValid.getOrThrow();

        String clientSecret = AESUtil.generateSecret();
        String encryptedClientSecret = AESUtil.encrypt(clientSecret, secretKey);

        ClientAccessInfo clientAccessInfo = clientAccessInfoRepository.save(ClientAccessInfo.of(dto, encryptedClientSecret, customerCompany));

        return ClientAccessInfoRes.of(clientAccessInfo, customerCompany, secretKey);

    }

    /**
     * 클라이언트 Secret-Key 재발급 합니다.
     *
     * @param clientAccessRegNo 클라이언트 접속 등록 번호
     * @return ClientAccessInfoRes
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
     * 클라이언트 접근권한을 삭제합니다.
     *
     * @param clientAccessRegNo 클라이언트 접속 등록번호
     * @return ClientAccessInfoRes
     */
    @Transactional
    public ClientAccessInfoRes deleteClientAccessInfo(Integer clientAccessRegNo) {

        ValidationResult<ClientAccessInfo> clientAccessInfoValid = clientAccessInfoValidator.ensureExists(clientAccessRegNo);
        ClientAccessInfo clientAccessInfo = clientAccessInfoValid.getOrThrow();

        clientAccessInfo.softDelete();

        return ClientAccessInfoRes.of(clientAccessInfo, clientAccessInfo.getCustomerCompany(), secretKey);

    }

    /**
     * 복호화된 클라이언트 Secret-Key를 조회
     *
     * @param clientAccessRegNo 클라이언트 접속 등록 번호
     * @return String
     */
    public ClientAccessSecretRes getClientAccessInfoSecret(Integer clientAccessRegNo) {

        ValidationResult<ClientAccessInfo> clientAccessInfoValid = clientAccessInfoValidator.ensureExists(clientAccessRegNo);
        ClientAccessInfo clientAccessInfo = clientAccessInfoValid.getOrThrow();

        return ClientAccessSecretRes.of(AESUtil.decrypt(clientAccessInfo.getClientSecretKey(), secretKey));

    }

}
