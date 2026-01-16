package com.sk.domain.mi.dto.response.news;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record TopicTrackerListRes(
    @Schema(description = "뉴스 토픽 관계 ID", example = "1001")
    Long newsTopicRelationshipId,

    @Schema(description = "뉴스 요약 ID", example = "11")
    Long newsSummaryId,

    @Schema(description = "기준 일자", example = "20250729")
    String criteriaDate,

    @Schema(description = "뉴스 발행일자", example = "2025-07-29")
    String publishDate,

    @Schema(description = "뉴스 발행시간", example = "23:59")
    String publishTime,

    @Schema(description = "토픽 이름", example = "Energy Policy")
    String topicName,

    @Schema(description = "토픽 ID", example = "101")
    Long topicId,

    @Schema(description = "프로바이더 이름", example = "reuters")
    String providerName,

    @Schema(description = "지역", example = "[\"APAC\", \"NAM\"]")
    List<String> regions,

    @Schema(description = "간략 요약", example = "OPEC+ signaled a potential shift in output targets...")
    String briefSummary,

    @Schema(description = "뉴스 제목", example = "Approval to Amend Term of Warrants")
    String title,

    @Schema(description = "요약", example = "[\"Line1\", \"Line2\"]")
    List<String> summary,

    @Schema(description = "간략 요약 영문", example = "OPEC+ signaled a potential shift in output targets...")
    String briefSummaryEn
) {

}
