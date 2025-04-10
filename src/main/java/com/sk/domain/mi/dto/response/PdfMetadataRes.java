package com.sk.domain.mi.dto.response;

public record PdfMetadataRes(

    String fileNo,
    String fileName,
    String filePath,
    String fileSize,
    Integer numberOfImages

) {
}
