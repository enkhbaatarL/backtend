package com.sk.domain.mi.dto.response.fundamentalmodel;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record FundamentalModelCaseFilesRes(

    @ArraySchema(schema = @Schema(implementation = FundamentalModelDownloadFileRes.class))
    List<FundamentalModelDownloadFileRes> input,

    @ArraySchema(schema = @Schema(implementation = FundamentalModelDownloadFileRes.class))
    List<FundamentalModelDownloadFileRes> output,

    @ArraySchema(schema = @Schema(implementation = FundamentalModelDownloadFileRes.class))
    List<FundamentalModelDownloadFileRes> intermediate

) {
}
