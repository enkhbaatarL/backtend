package com.sk.domain.admin.dto.request.company;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record CompanyReq(

) {

    @Schema(name = "CompanyReq.Insert")
    public record Insert(

        @NotEmpty
        @Schema(description = "고객회사 코드", example = "159")
        String companyCd,

        @NotNull
        @JsonFormat(pattern = "yyyyMMdd")
        @DateTimeFormat(pattern = "yyyyMMdd")
        @Schema(description = "사용 시작일자", example = "20250101")
        LocalDate usageBeginDate,

        @NotNull
        @JsonFormat(pattern = "yyyyMMdd")
        @DateTimeFormat(pattern = "yyyyMMdd")
        @Schema(description = "사용 종료일자", example = "20251231")
        LocalDate usageEndDate

    ) {
    }

    @Schema(name = "CompanyReq.Update")
    public record Update(

        @Schema(description = "활성화 여부", example = "true")
        Boolean isActivated,

        @JsonFormat(pattern = "yyyyMMdd")
        @DateTimeFormat(pattern = "yyyyMMdd")
        @Schema(description = "사용 시작일자", example = "20250101")
        LocalDate usageBeginDate,

        @JsonFormat(pattern = "yyyyMMdd")
        @DateTimeFormat(pattern = "yyyyMMdd")
        @Schema(description = "사용 종료일자", example = "20251231")
        LocalDate usageEndDate

    ) {
    }

    @Schema(name = "CompanyReq.OwnerInsert")
    public record OwnerInsert(

        @NotEmpty
        @Schema(description = "고객회사 코드", example = "159")
        String companyCd

    ) {
    }

}
