package com.sk.domain.admin.dto.request.company;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

// @formatter:off
public record CompanyReq(

) {

    @Schema(name = "companyInsert")
    public record Insert(

            @NotEmpty
            @Schema(description = "고객회사 코드", example = "159")
            String companyCd,

            @NotEmpty
            @Schema(description = "고객회사 명", example = "SK이노베이션")
            String companyNm,

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

    @Schema(name = "companyUpdate")
    public record Update(

            @Schema(description = "활성화 여부", example = "true")
            Boolean isActivated,

            @Schema(description = "고객회사 명", example = "SK이노베이션")
            String companyNm,

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

}
// @formatter:on