package com.sk.domain.mi.dto.response.newsletter;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record NewsletterIssueListRes(
       @Schema(description = "뉴스레터 ID", example = "NL001")
        String newsletterId,

        @Schema(description = "발행목록")
        List<IssuedItem> issuedList) {

    public record IssuedItem(
            @Schema(description = "발행일자", example = "25-09-12")
            String date,

            @Schema(description = "발행번호", example = "102")
            String no) {
    }
}