package com.sk.domain.mi.dto.response.news;

import io.swagger.v3.oas.annotations.media.Schema;

public record TopicInfoRes(
    @Schema(description = "뉴스 토픽 ID", example = "1")
    Long newsTopicId,

    @Schema(description = "토픽 아이콘 이미지 경로", example = "/images/topics/trump-tariff.png")
    String iconImagePath,

    @Schema(description = "뉴스 토픽명", example = "Trump Tariff")
    String newsTopicName
) {

    public static TopicInfoRes of(Long newsTopicId, String iconImagePath, String newsTopicName) {
        return new TopicInfoRes(newsTopicId, iconImagePath, newsTopicName);
    }
}
