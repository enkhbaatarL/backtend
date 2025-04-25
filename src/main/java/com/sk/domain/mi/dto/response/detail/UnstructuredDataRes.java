package com.sk.domain.mi.dto.response.detail;

import com.sk.domain.shared.dto.AuditInfo;
import io.swagger.v3.oas.annotations.media.Schema;

public record UnstructuredDataRes(

    @Schema(description = "파일 번호", example = "100001")
    Integer fileNo,

    @Schema(description = "파일 명", example = "data-name.xlsx")
    String fileName,

    @Schema(description = "확장자", example = "pptx")
    String extension,

    @Schema(description = "PDF 미리보기 가능 여부", example = "false")
    Boolean isPdfViewable,

    AuditInfo auditInfo

) {
}
