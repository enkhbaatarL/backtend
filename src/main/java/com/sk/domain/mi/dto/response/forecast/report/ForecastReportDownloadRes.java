package com.sk.domain.mi.dto.response.forecast.report;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

public record ForecastReportDownloadRes(
        @Schema(description = "리포트 목록") List<ForecastReport> reports,
        @Schema(description = "파일 다운로드 여부") boolean isFileDownload) {

    @Builder
    public ForecastReportDownloadRes(List<ForecastReport> reports, boolean isFileDownload) {
        this.reports = reports;
        this.isFileDownload = isFileDownload;
    }

    public static ForecastReportDownloadRes of(List<ForecastReport> reports,
            boolean isFileDownload) {
        return ForecastReportDownloadRes.builder()
                .reports(reports)
                .isFileDownload(isFileDownload)
                .build();
    }
}
