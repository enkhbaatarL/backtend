package com.sk.domain.mi.dto.request.nextgen.generation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * [MCP Event Generate] 사용자 입력 문장 기반 이벤트 생성 요청. 이벤트 생성 서버(POST /events/generations) body 와 같은 shape 로 그대로 전달한다.
 * 필드 description 은 그대로 MCP tool 인자 설명이 된다.
 */
@Schema(description = "사용자 입력 문장 기반 이벤트 생성 요청")
public record EventGenerationReq(

    @Schema(description = "이벤트를 만들고 싶은 주제를 담은 사용자 입력 문장. 사용자의 요청 문장을 요약하지 말고 그대로 넣습니다.",
        example = "최근 인도 LPG 수입 정책 변화로 이벤트를 만들어줘", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "사용자 입력 문장은 필수입니다.")
    String userQuery,

    @Schema(description = "근거 조회 시작일(YYYY-MM-DD). 사용자가 기간을 말하지 않았으면 생략합니다.", example = "2026-09-01")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "조회 시작일은 YYYY-MM-DD 형식입니다.")
    String dateFrom,

    @Schema(description = "근거 조회 종료일(YYYY-MM-DD). 사용자가 기간을 말하지 않았으면 생략합니다.", example = "2026-10-12")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "조회 종료일은 YYYY-MM-DD 형식입니다.")
    String dateTo

) {}
