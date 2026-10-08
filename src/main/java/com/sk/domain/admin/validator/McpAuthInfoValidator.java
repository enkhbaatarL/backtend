package com.sk.domain.admin.validator;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.ValidationResult;
import com.sk.domain.shared.entity.master.tmd.McpAuthInfo;
import com.sk.domain.shared.repository.master.tmd.McpAuthInfoRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** [MCP 인증] MCP 인증 키 존재·ID 중복 검사. */
@Component
@RequiredArgsConstructor
public class McpAuthInfoValidator {

    private final McpAuthInfoRepository mcpAuthInfoRepository;

    public ValidationResult<McpAuthInfo> ensureExists(Integer mcpAuthRegNo) {
        Optional<McpAuthInfo> entity = mcpAuthInfoRepository.findByMcpAuthRegNoAndIsDeletedFalse(mcpAuthRegNo);
        if (entity.isPresent()) {
            return ValidationResult.success(entity.get());
        } else {
            return ValidationResult.failure(AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                    "It doesn't exist in the [McpAuthInfo]. mcpAuthRegNo: %s".formatted(mcpAuthRegNo)));
        }
    }

    public ValidationResult<Void> ensureIdNotUsed(String mcpAuthId) {
        if (mcpAuthInfoRepository.existsByMcpAuthId(mcpAuthId)) {
            return ValidationResult.failure(AppErrorException.of(ResultCode.Error.RSC_ALREADY_EXISTS,
                    "McpAuthId [%s] already exists.".formatted(mcpAuthId)));
        } else {
            return ValidationResult.success(null);
        }
    }

}
