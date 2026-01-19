package com.sk.domain.mi.dto.response.news;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record NewsTopicListRes(
    @Schema(description = "뉴스 토픽 관계 ID", example = "11")
    Long newsTopicRelationshipId,

    @Schema(description = "기준 일자", example = "11")
    String crtrDt,

    @Schema(description = "뉴스 토픽 ID", example = "11")
    Long newsTopicId,

    @Schema(description = "뉴스 토픽명", example = "Trump Tariff")
    String newsTopicName,

    @Schema(description = "뉴스 요약 ID", example = "11")
    Integer newsSummaryId,

    @Schema(description = "데이터등록일련번호", example = "36033981")
    Integer dataRegistrationSn,

    @Schema(description = "NEWS최초생성일시", example = "2025-07-29 23:59:09")
    LocalDateTime firstCreated,

    @Schema(description = "간략요약", example = "")
    String briefSummary,

    @Schema(description = "요약", example = "")
    List<String> summary,

    @Schema(description = "뉴스 제목", example = "Approval to Amend Term of Warrants")
    String title,

    @Schema(description = "프로바이더", example = "reuters")
    String provider,

    @Schema(description = "보기 권한 여부", example = "true")
    boolean isViewable,

    @Schema(description = "지역", example = "[Global, Asia Pacific]")
    List<String> regions

) {

}
