package com.sk.domain.admin.dto.request.provider;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record StructuredDataFileReq(

        @NotBlank @Schema(description = "컴포넌트 타입 ID", example = "TYPE_ID") String componentTypeId,

        @Schema(description = "엑셀 파일에는 없지만 TABLE에는 기준일자가 있을 때 사용, 테이블 칼럼명 = crtr_dt", example = "20250424") String criteriaDt
) {
}
