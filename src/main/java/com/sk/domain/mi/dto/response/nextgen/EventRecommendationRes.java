package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record EventRecommendationRes(
    @Schema(description = "전체 선택에 대한 추천 Event (0~2개)")
    @NotNull @Size(max = 2) List<@NotNull @Valid Item> items,
    @NotNull String selectionReason
) {
    public record Item(@NotNull @Positive Long miEventId, @NotNull String title, @NotNull String reason) {}
}
