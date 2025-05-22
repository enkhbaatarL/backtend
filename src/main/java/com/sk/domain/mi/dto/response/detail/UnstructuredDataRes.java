package com.sk.domain.mi.dto.response.detail;

import com.sk.domain.shared.dto.AuditInfo;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.enums.Extension;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

// @formatter:off
public record UnstructuredDataRes(

        @Schema(description = "파일 번호", example = "100001") 
        Long fileNo,

        @Schema(description = "파일 명", example = "data-name.xlsx") 
        String fileName,

        @Schema(description = "파일 크기", example = "12345") 
        Long fileSize,

        @Schema(description = "확장자", example = "pptx") 
        String extension,

        @Schema(description = "PDF 미리보기 가능 여부(PDF 타입의 파일이여도 '이미지 변환'이 완료되지 않은 경우에는 미리보기가 지원되지 않음)", example = "false") Boolean isPdfViewable,

        AuditInfo auditInfo

) {
    @Builder
    public UnstructuredDataRes(Long fileNo, String fileName, Long fileSize, String extension, Boolean isPdfViewable, AuditInfo auditInfo) {
        this.fileNo = fileNo;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.extension = extension;
        this.isPdfViewable = isPdfViewable;
        this.auditInfo = auditInfo;
    }

    public static UnstructuredDataRes of(UploadedFile entity) {
        return UnstructuredDataRes.builder()
                .fileNo(entity.getFileNo())
                .fileName(entity.getOriginalFileNm())
                .fileSize(entity.getFileSize())
                .extension(entity.getFileExtNm())
                .isPdfViewable(Extension.isPdfViewable(entity.getFileExtNm()) && entity.getIsImageConversionCompleted())
                .auditInfo(AuditInfo.ofSimple(
                        entity.getCreateUserId(),
                        entity.getCreateDt(),
                        entity.getModifyUserId(),
                        entity.getModifyDt()))
                .build();
    }

    public static List<UnstructuredDataRes> ofList(List<UploadedFile> entities) {
        return entities.stream()
                .map(UnstructuredDataRes::of)
                .toList();
    }

}
// @formatter:on
