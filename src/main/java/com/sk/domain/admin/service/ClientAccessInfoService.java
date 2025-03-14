package com.sk.domain.admin.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.admin.dto.request.ClientAccessInfoReq;
import com.sk.domain.admin.dto.response.ClientAccessInfoRes;
import com.sk.domain.shared.entity.master.tif.ClientAccessInfo;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.repository.master.tif.ClientAccessInfoRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ClientAccessInfoService {

    private final CustomerCompanyRepository customerCompanyRepository;
    private final ClientAccessInfoRepository clientAccessInfoRepository;

    /**
     * 클라이언트 접근권한 목록을 조회한다.
     *   - filter : isDeleted false
     *
     * @return List<ClientAccessInfoRes>
     */
    public List<ClientAccessInfoRes> getClientAccessInfos() {
        List<ClientAccessInfo> clientAccessInfos = clientAccessInfoRepository.findByIsDeletedFalse();
        return clientAccessInfos.stream().map(ClientAccessInfoRes::of).toList();
    }

    /**
     * 클라이언트 접근권한을 등록합니다.
     *
     * @param dto ClientAccessInfoReq.Insert
     * @return ClientAccessInfoRes
     */
    public ClientAccessInfoRes postClientAccessInfo(ClientAccessInfoReq.Insert dto) {

        // customerCompany 검증
        customerCompanyRepository.findByCustomerCompanyNoAndIsDeletedFalse(dto.customerCompanyNo())
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.REQ_MISSING_PARAMETER, String.format("requested id: %s", dto.customerCompanyNo())));

        return ClientAccessInfoRes.of(clientAccessInfoRepository.save(ClientAccessInfo.of(dto)));

    }

    /**
     * 클라이언트 접근권한을 수정합니다.
     *
     * @param dto ClientAccessInfoReq.Update
     * @return ClientAccessInfoRes
     */
    public ClientAccessInfoRes patchClientAccessInfo(ClientAccessInfoReq.Update dto) {

        ClientAccessInfo clientAccessInfo = clientAccessInfoRepository.findByClientAccessRegNoAndIsDeletedFalse(dto.clientAccessRegNo())
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.REQ_MISSING_PARAMETER, String.format("requested id: %s", dto.clientAccessRegNo())));

        CustomerCompany customerCompany = null;
        if (ObjectUtils.isNotEmpty(dto.customerCompanyNo())) {
            customerCompany = customerCompanyRepository.findByCustomerCompanyNoAndIsDeletedFalse(dto.customerCompanyNo())
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.REQ_MISSING_PARAMETER, String.format("requested id: %s", dto.customerCompanyNo())));
        }

        clientAccessInfo.update(dto, customerCompany);

        return ClientAccessInfoRes.of(clientAccessInfo);

    }

    /**
     * 클라이언트 접근권한을 삭제합니다.
     *
     * @param clientAccessRegNo 클라이언트 접속 등록번호
     * @return ClientAccessInfoRes
     */
    public ClientAccessInfoRes deleteClientAccessInfo(Integer clientAccessRegNo) {

        ClientAccessInfo clientAccessInfo = clientAccessInfoRepository.findByClientAccessRegNoAndIsDeletedFalse(clientAccessRegNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.REQ_MISSING_PARAMETER, String.format("requested id: %s", clientAccessRegNo)));

        clientAccessInfo.softDelete();

        return ClientAccessInfoRes.of(clientAccessInfo);

    }

}
