package com.sk.domain.mi.dto.request.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

@Builder
public record QnaReq(

) {

    @Builder
    @Schema(name = "QnaReq.Insert")
    public record Insert(

        @NotEmpty
        @Schema(description = "질의문", example = "삼성전자 작년 실적을 비교해줘")
        String question,

        @Schema(description = "웹서치 여부", example = "true")
        Boolean shouldWebSearch

    ) {

        public static Insert ofDefault(String userQuestionCntt) {
            return Insert.builder()
                .question(userQuestionCntt)
                .shouldWebSearch(true)
                .build();
        }
    }

}
