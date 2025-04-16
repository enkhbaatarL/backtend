package com.sk.domain.admin.dto.request.provider;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record StructuredDataFileReq(

        @NotBlank @Schema(description = "컴포넌트 타입 ID", example = "TYPE_ID") String componentTypeId

) {
}
