package com.sk.domain.mi.dto.response.newsletter;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record NewsletterSubscriptionRes(
    @Schema(description = "뉴스레터 고유 ID", example = "1")
    String newsletterId,

    @Schema(description = "구독 여부", example = "true")
    Boolean subscribed,

    @Schema(description = "구독일시", example = "2026-03-11T14:25:00")
    LocalDateTime subscribedAt,

    @Schema(description = "구독해지일시", example = "2026-03-11T15:25:00")
    LocalDateTime unsubscribedAt
) {
}
