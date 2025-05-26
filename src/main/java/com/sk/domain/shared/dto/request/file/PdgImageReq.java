package com.sk.domain.shared.dto.request.file;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

public record PdgImageReq(

    @Schema(description = "이미지로 변환할 PDF 파일 번호 리스트. null 또는 비어있으면 전체 파일을 대상으로 한다.", example = "[1, 23, 456]")
    List<Long> fileNos,

    @Schema(description = "날짜(포함) 이후에 생성된 파일만 대상으로 한다. null이면 전체 파일 대상", example = "20250501")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
    @DateTimeFormat(pattern = "yyyyMMdd")
    LocalDate createdAfter

) {
}
