package com.sk.domain.admin.dto.request.user;

import java.util.List;

import com.sk.domain.shared.enums.UserRoleCd;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record AdminReq() {
    @Schema(name = "adminInsert")
    public record Insert(
        @NotEmpty(message = "IM코드는 필수 입력 사항입니다.") 
        @Schema(description = "IM코드", example = "10001", required = true)
        String imCd,

        @Schema(description = "타사서비스접근가능여부", example = "false", defaultValue = "false") 
        Boolean isOtherServiceAccessPossible,

        @ArraySchema(
            schema = @Schema(type = "string", example = "192.168.1.1"),
            arraySchema = @Schema(description = "허용된 IP 주소 목록")
        )
        List<String> accessAllowedIpAddress,

        @Schema(description = "Role", example = "ADMIN or OPERATOR")
        UserRoleCd userRoleCd
    ) {
        public Insert {
            isOtherServiceAccessPossible = (isOtherServiceAccessPossible == null) ? false
                    : isOtherServiceAccessPossible;
        }
    }

    @Schema(name = "adminUpdate")
    public record Update(

        @NotNull(message = "타사서비스접근가능여부는 필수 입력 사항입니다.")
        @Schema(description = "타사서비스접근가능여부", example = "false") 
        Boolean isOtherServiceAccessPossible,

        @ArraySchema(
            schema = @Schema(type = "string", example = "192.168.1.1"),
            arraySchema = @Schema(description = "허용된 IP 주소 목록")
        )
        List<String> accessAllowedIpAddress
    ) {
    }
}
