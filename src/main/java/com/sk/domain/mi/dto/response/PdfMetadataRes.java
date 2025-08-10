package com.sk.domain.mi.dto.response;

import com.sk.domain.shared.dto.AuditInfo;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record PdfMetadataRes(

    @Schema(description = "파일 번호", example = "123")
    Long fileNo,

    @Schema(description = "원본 파일 명", example = "abc.pdf")
    String originFileNm,

    @Schema(description = "파일 사이즈", example = "12345")
    Long fileSize,

    @Schema(description = "파일 페이지 수(이미지)", example = "56")
    Integer numberOfImages,

    @Schema(description = "이미지 변환 작업 완료 여부", example = "true")
    Boolean isImageConversionCompleted,

    AuditInfo auditInfo

) {

    @Builder
    public PdfMetadataRes(Long fileNo, String originFileNm, Long fileSize, Integer numberOfImages, Boolean isImageConversionCompleted, AuditInfo auditInfo) {
        this.fileNo = fileNo;
        this.originFileNm = originFileNm;
        this.fileSize = fileSize;
        this.numberOfImages = numberOfImages;
        this.isImageConversionCompleted = isImageConversionCompleted;
        this.auditInfo = auditInfo;
    }

    public static PdfMetadataRes of(UploadedFile entity) {
        return PdfMetadataRes.builder()
            .fileNo(entity.getFileNo())
            .originFileNm(entity.getOriginalFileNm())
            .fileSize(entity.getFileSize())
            .numberOfImages(entity.getFilePageCnt())
            .isImageConversionCompleted(entity.getIsImageConversionCompleted())
            .auditInfo(AuditInfo.ofSimple(
                entity.getCreateUserId(),
                entity.getCreateDt(),
                entity.getModifyUserId(),
                entity.getModifyDt()
            ))
            .build();
    }

}
