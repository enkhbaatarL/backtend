package com.sk.domain.shared.dto.request.common;

import com.sk.domain.shared.enums.FeedbackTypeCd;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "사용자 피드백 등록 요청")
public record UserFeedbackReq(

    @Schema(description = "피드백 대상 페이지명", example = "Next Gen", maxLength = 50)
    @Size(max = 50, message = "피드백 페이지명은 50자 이하여야 합니다.")
    String feedbackPageNm,

    @Schema(description = "피드백 대상 섹션명", example = "Overview", maxLength = 50)
    @Size(max = 50, message = "피드백 섹션명은 50자 이하여야 합니다.")
    String feedbackSectionNm,

    @Schema(description = "피드백 유형 코드", example = "data_error")
    @NotNull(message = "피드백 유형 코드는 필수입니다.")
    FeedbackTypeCd feedbackTypeCd,

    @Schema(description = "피드백 내용", example = "데이터가 정확하지 않습니다.", maxLength = 2000)
    @Size(max = 2000, message = "피드백 내용은 2000자 이하여야 합니다.")
    String feedbackContent

) {}
