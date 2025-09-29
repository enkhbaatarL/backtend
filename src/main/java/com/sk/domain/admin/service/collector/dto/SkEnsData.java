package com.sk.domain.admin.service.collector.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

// @formatter:off
public record SkEnsData(
){
    @Schema($schema = "SkEnsData.Code")
    public record Code(
        @NotBlank
        String code,
        @NotBlank
        List<Data> data
    ){}

    @Schema($schema = "SkEnsData.Data")
    public record Data(
        @NotBlank
        String date,

        @NotBlank
        String value
    ){}
}
// @formatter:on
