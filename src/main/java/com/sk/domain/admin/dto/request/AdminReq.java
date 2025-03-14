package com.sk.domain.admin.dto.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record AdminReq() {
    public record Insert(

        @NotEmpty(message = "IM코드는 필수 입력 사항입니다.") 
        @Schema(description = "IM코드", example = "10001") 
        String imCd,

        @Schema(description = "타사서비스접근가능여부", example = "false") 
        Boolean isOtherServiceAccessPossible,

        @Schema(description = "접근허용 IP", example = "['192.168.1.1','192.168.1.2']")
        List<String> accessAllowedIpAddress
    ) {
        public Insert {
            isOtherServiceAccessPossible = (isOtherServiceAccessPossible == null) ? false
                    : isOtherServiceAccessPossible;
        }
    }

    public record Update(

        @NotNull(message = "userNo는 필수 입력 사항입니다.")
        @Schema(description = "userNo", example = "1")
        Integer userNo,

        @Schema(description = "타사서비스접근가능여부", example = "false") 
        Boolean isOtherServiceAccessPossible,

        @Schema(description = "접근허용 IP", example = "[\"192.168.1.1\", \"192.168.1.2\"]")
        List<String> accessAllowedIpAddress
    ) {
    }
}
