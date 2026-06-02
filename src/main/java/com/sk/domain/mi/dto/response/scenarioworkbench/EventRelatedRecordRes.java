package com.sk.domain.mi.dto.response.scenarioworkbench;

import com.sk.domain.shared.repository.master.tmm.projection.EventRecordRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record EventRelatedRecordRes(

    @Schema(description = "Record 발행/생성 일시")
    LocalDateTime publishedAt,

    @Schema(description = "Record 종류 (NEWS, REPORT 등)", example = "NEWS")
    String recordType,

    @Schema(description = "제목")
    String title,

    @Schema(description = "요약")
    String summary,

    @Schema(description = "상품 번호", example = "100")
    Integer goodsNo,

    @Schema(description = "상품 데이터 번호. NEWS인 경우 null", example = "200")
    Integer goodsDataNo,

    @Schema(description = "원본 소스 ID", example = "12345")
    Long sourceObjId

) {

    public static EventRelatedRecordRes from(EventRecordRow row) {
        return new EventRelatedRecordRes(
            row.getFirstCreated(),
            row.getRecordType(),
            row.getTitle(),
            row.getSummary(),
            row.getGoodsNo(),
            row.getGoodsDataNo(),
            row.getSourceObjId()
        );
    }
}
