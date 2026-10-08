package com.sk.domain.admin.service;

import com.sk.common.config.security.mcp.McpSecretHasher;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.request.mcp.McpAuthInfoReq;
import com.sk.domain.admin.dto.response.mcp.McpAuthExistsRes;
import com.sk.domain.admin.dto.response.mcp.McpAuthInfoRes;
import com.sk.domain.admin.dto.response.mcp.McpAuthIssueRes;
import com.sk.domain.admin.validator.CompanyValidator;
import com.sk.domain.admin.validator.McpAuthInfoValidator;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.McpAuthInfo;
import com.sk.domain.shared.repository.master.tmd.McpAuthInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [MCP 인증] MCP API Basic Auth 키 관리. 시크릿은 일방향(PBKDF2) 저장이라 다시 조회할 수 없고, 발급·재발급 응답에서만 평문을 내려줍니다.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminMcpAuthService {

    private final CompanyValidator companyValidator;
    private final McpAuthInfoRepository mcpAuthInfoRepository;
    private final McpAuthInfoValidator mcpAuthInfoValidator;
    private final McpSecretHasher mcpSecretHasher;

    /**
     * MCP 인증 키 목록을 조회합니다. 생성일시 내림차순.
     */
    public PaginationRes<McpAuthInfoRes> getMcpAuthInfos(int page, int size) {
        return PaginationRes.toRes(mcpAuthInfoRepository.findMcpAuthInfoByConditions(page, size));
    }

    /**
     * ID 사용 여부를 확인합니다. 화면의 중복 확인용이며, 등록 시에도 서버에서 다시 검사합니다.
     */
    public McpAuthExistsRes existsMcpAuthId(String mcpAuthId) {
        return McpAuthExistsRes.of(mcpAuthInfoRepository.existsByMcpAuthId(mcpAuthId));
    }

    /**
     * MCP 인증 키를 등록합니다.
     */
    @Transactional
    public McpAuthIssueRes postMcpAuthInfo(McpAuthInfoReq.Insert dto) {

        CustomerCompany customerCompany = companyValidator.ensureCompanyExistsByCompanyNo(dto.customerCompanyNo()).getOrThrow();
        mcpAuthInfoValidator.ensureIdNotUsed(dto.mcpAuthId()).getOrThrow();

        String secret = mcpSecretHasher.generateSecret();
        McpAuthInfo mcpAuthInfo;
        try {
            mcpAuthInfo = mcpAuthInfoRepository.saveAndFlush(McpAuthInfo.of(dto.mcpAuthId(), mcpSecretHasher.hash(secret), customerCompany));
        } catch (DataIntegrityViolationException e) {
            // 동시 등록으로 사전 검사를 함께 통과한 경우 DB UNIQUE 제약이 막는다
            throw AppErrorException.of(ResultCode.Error.RSC_ALREADY_EXISTS, "McpAuthId [%s] already exists.".formatted(dto.mcpAuthId()));
        }

        return McpAuthIssueRes.of(McpAuthInfoRes.of(mcpAuthInfo, customerCompany), secret);
    }

    /**
     * 시크릿을 재발급합니다. 이전 시크릿은 즉시 사용할 수 없게 됩니다.
     */
    @Transactional
    public McpAuthIssueRes patchMcpAuthInfo(Integer mcpAuthRegNo) {

        McpAuthInfo mcpAuthInfo = mcpAuthInfoValidator.ensureExists(mcpAuthRegNo).getOrThrow();

        String secret = mcpSecretHasher.generateSecret();
        mcpAuthInfo.updateSecret(mcpSecretHasher.hash(secret));
        mcpAuthInfoRepository.flush();

        return McpAuthIssueRes.of(McpAuthInfoRes.of(mcpAuthInfo, mcpAuthInfo.getCustomerCompany()), secret);
    }

    /**
     * 논리 삭제합니다. ID 는 d_{등록번호}_{원래ID} 로 바뀌어 같은 ID 로 재등록할 수 있습니다.
     */
    @Transactional
    public McpAuthInfoRes deleteMcpAuthInfo(Integer mcpAuthRegNo) {

        McpAuthInfo mcpAuthInfo = mcpAuthInfoValidator.ensureExists(mcpAuthRegNo).getOrThrow();
        mcpAuthInfo.delete();
        mcpAuthInfoRepository.flush();

        return McpAuthInfoRes.of(mcpAuthInfo, mcpAuthInfo.getCustomerCompany());
    }

}
