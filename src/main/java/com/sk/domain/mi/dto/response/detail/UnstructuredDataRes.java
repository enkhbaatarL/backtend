package com.sk.domain.mi.dto.response.detail;

import com.sk.domain.shared.dto.AuditInfo;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.enums.Extension;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
public record UnstructuredDataRes(

    @Schema(description = "파일 번호", example = "100001")
    Long fileNo,

    @Schema(description = "파일 명", example = "data-name.xlsx")
    String fileName,

    @Schema(description = "확장자", example = "pptx")
    String extension,

    @Schema(description = "PDF 미리보기 가능 여부", example = "false")
    Boolean isPdfViewable,

    AuditInfo auditInfo

) {

    public static UnstructuredDataRes of(UploadedFile entity) {
        return UnstructuredDataRes.builder()
            .fileNo(entity.getFileNo())
            .fileName(entity.getOriginalFileNm())
            .extension(entity.getFileExtNm())
            .isPdfViewable(Extension.isPdfViewable(entity.getFileExtNm()))
            .auditInfo(AuditInfo.ofSimple(
                entity.getCreateUserId(),
                entity.getCreateDt(),
                entity.getModifyUserId(),
                entity.getModifyDt()
            ))
            .build();
    }

    public static List<UnstructuredDataRes> ofList(List<UploadedFile> entities) {
        return entities.stream()
            .map(UnstructuredDataRes::of)
            .toList();
    }

}
