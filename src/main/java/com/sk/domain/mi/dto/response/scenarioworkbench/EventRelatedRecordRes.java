package com.sk.domain.mi.dto.response.scenarioworkbench;

import com.sk.domain.shared.repository.master.tmm.projection.EventRecordRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record EventRelatedRecordRes(

    @Schema(description = "Record 생성일시")
    LocalDateTime publishedAt,

    @Schema(description = "Record 유형", example = "NEWS")
    String recordType,

    @Schema(description = "제목")
    String title,

    @Schema(description = "요약")
    String summary,

    @Schema(description = "상품번호")
    Integer goodsNo,

    @Schema(description = "파일명")
    String fileName

) {

    public static EventRelatedRecordRes from(EventRecordRow row) {
        return new EventRelatedRecordRes(
            row.getFirstCreated(),
            row.getRecordType(),
            row.getTitle(),
            row.getSummary(),
            row.getGoodsNo(),
            row.getFileName()
        );
    }
}
