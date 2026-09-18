package com.sk.domain.mi.dto.response.fundamentalmodel;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record FundamentalModelCaseFilesRes(

    @Schema(description = "case 입력 파일명 목록")
    @ArraySchema(schema = @Schema(implementation = FundamentalModelFileNameRes.class))
    List<FundamentalModelFileNameRes> input,

    @Schema(description = "최신 실행 output 파일의 다운로드 metadata 목록. 최신 실행 또는 연결 파일이 없으면 빈 배열입니다.")
    @ArraySchema(schema = @Schema(implementation = FundamentalModelDownloadFileRes.class))
    List<FundamentalModelDownloadFileRes> output,

    @Schema(description = "최신 실행 중간파일의 다운로드 metadata 목록. 최신 실행 또는 연결 파일이 없으면 빈 배열입니다.")
    @ArraySchema(schema = @Schema(implementation = FundamentalModelDownloadFileRes.class))
    List<FundamentalModelDownloadFileRes> intermediate,

    @Schema(
        description = "최신 실행과 같은 run_seq로 연결된 Trade Flow 결과파일의 다운로드 metadata 목록. 최신 실행, run_seq 또는 연결 파일이 없으면 빈 배열입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @ArraySchema(schema = @Schema(implementation = FundamentalModelDownloadFileRes.class))
    @NotNull
    List<FundamentalModelDownloadFileRes> tradeOutput

) {
}
