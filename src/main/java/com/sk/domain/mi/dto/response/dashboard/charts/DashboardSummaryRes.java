package com.sk.domain.mi.dto.response.dashboard.charts;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record DashboardSummaryRes(
        @Schema(description = "설명력제목내용") List<DashboardSummary> summaries,

        @Schema(description = "파일 다운로드 여부") boolean isFileDownload) {

    @Builder
    public DashboardSummaryRes(List<DashboardSummary> summaries, boolean isFileDownload) {
        this.summaries = summaries;
        this.isFileDownload = isFileDownload;
    }

    public static DashboardSummaryRes of(List<DashboardSummary> summaries, boolean isFileDownload) {
        return DashboardSummaryRes.builder()
                .summaries(summaries)
                .isFileDownload(isFileDownload)
                .build();
    }
}
