package com.sk.domain.mi.dto.response.nextgen;

import com.sk.common.utils.DateUtil;
import com.sk.domain.shared.repository.master.tmm.projection.EventRecordMappingRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventRecordRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record EventRelatedRecordRes(

    @Schema(description = "Record 발행/생성 일시 (KST). 시각이 00:00:00이면 날짜만 표기", example = "2025-07-29 23:59:09")
    String publishedAt,

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
    Long sourceObjId,

    @Schema(description = "권한 여부", example = "true")
    Boolean isPermitted,

    @Schema(description = "이벤트-레코드 매핑 근거", example = "해당 기사는 이벤트의 공급 차질 경로를 직접 서술한다")
    String mappingReason

) {

    public static EventRelatedRecordRes from(EventRecordRow row, boolean isPermitted, EventRecordMappingRow mapping) {
        return new EventRelatedRecordRes(
            toKstPublishedAt(row.getFirstCreated()),
            row.getRecordType(),
            row.getTitle(),
            row.getSummary(),
            row.getGoodsNo(),
            row.getGoodsDataNo(),
            row.getSourceObjId(),
            isPermitted,
            mapping == null ? null : mapping.getMatchReason()
        );
    }

    /**
     * MI Record 생성시간을 UTC로 해석해 KST로 변환.
     *   - 변환 후 시각이 자정(00:00:00)이면 날짜만, 아니면 시각까지 표기

     */
    private static String toKstPublishedAt(LocalDateTime utc) {
        LocalDateTime kst = DateUtil.utcToKst(utc);
        if (kst == null) {
            return null;
        }
        return kst.toLocalTime().equals(LocalTime.MIDNIGHT) ? kst.format(DateUtil.YYYY_MM_DD) : kst.format(DateUtil.YYYY_MM_DD_HH_MM_SS);
    }

}
