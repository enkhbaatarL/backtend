package com.sk.domain.mi.dto.request.newsletter;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NewsletterMailSendReq {
        @Schema(description = "메일 제목", requiredMode = Schema.RequiredMode.REQUIRED)
        private String subject;

        @Schema(description = "메일 HTML 본문", requiredMode = Schema.RequiredMode.REQUIRED)
        private String htmlBody;

        @Schema(description = "수신자 이메일 목록. multipart key: recipientEmails (반복 전달), 미입력 시 정책 fallback 적용")
        private List<String> recipientEmails;

        @Schema(description = "예약 발송 시각(ISO-8601). 과거/미입력은 즉시 발송으로 처리, 미래 값은 현재 API에서 미지원")
        private String scheduledAt;
}
