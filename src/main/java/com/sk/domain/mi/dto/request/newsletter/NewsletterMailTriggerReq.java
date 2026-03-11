package com.sk.domain.mi.dto.request.newsletter;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record NewsletterMailTriggerReq(

        @Schema(description = "메일 제목", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String subject
) {
}
