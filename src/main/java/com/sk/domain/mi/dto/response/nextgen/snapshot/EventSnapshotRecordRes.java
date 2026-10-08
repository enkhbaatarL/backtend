package com.sk.domain.mi.dto.response.nextgen.snapshot;

import com.sk.common.utils.DateUtil;
import com.sk.domain.shared.repository.master.tmm.projection.EventSnapshotRecordRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * [MCP Snapshot] Snapshot 근거 Record. 사본 컬럼 + 원본 Record 의 현재 값(식별용).
 */
public record EventSnapshotRecordRes(

    @Schema(description = "근거 Record ID. mi_record_id (사본)", example = "50123")
    Long miRecordId,

    @Schema(description = "Record 유형. record_type (사본)", example = "NEWS")
    String recordType,

    @Schema(description = "당시 Record 출처명. ref_nm (사본)", example = "Reuters", nullable = true)
    String refNm,

    @Schema(description = "Record 발생 시각 (KST). record_first_created (사본), 응답 시 KST 변환. 버전 저장 시각이 아님", example = "2026-07-19 20:00:00")
    LocalDateTime recordFirstCreated,

    @Schema(description = "이 버전의 저장 시각 (KST). Snapshot crt_dtm, 응답 시 KST 변환", example = "2026-09-15 14:02:53")
    LocalDateTime snapshotCreatedAt,

    @Schema(description = "제목 — 원본 Record 의 현재 값 (식별용). 원본이 없으면 null", nullable = true)
    String title,

    @Schema(description = "요약 — 원본 Record 의 현재 값 (식별용). 원본이 없으면 null", nullable = true)
    String summary,

    @Schema(description = "상품 번호 — 원본 Record 의 현재 값 (식별용)", example = "100", nullable = true)
    Integer goodsNo,

    @Schema(description = "상품 데이터 번호 — 원본 Record 의 현재 값 (식별용). NEWS 이면 null", example = "200", nullable = true)
    Integer goodsDataNo,

    @Schema(description = "원본 소스 ID — 원본 Record 의 현재 값 (식별용)", example = "12345", nullable = true)
    Long sourceObjId,

    @Schema(description = "요청 사용자(X-User-Id)의 상품 열람 권한 여부", example = "true")
    Boolean isPermitted,

    @Schema(description = "원본 Record·요약이 남아 있는지. 원본이 삭제됐으면 false 이며 사본 필드만 유효", example = "true")
    Boolean isSourceAvailable

) {

    public static EventSnapshotRecordRes from(EventSnapshotRecordRow row, LocalDateTime snapshotCreatedAtUtc, boolean isPermitted) {
        return new EventSnapshotRecordRes(
            row.getMiRecordId(),
            row.getRecordType(),
            row.getRefNm(),
            DateUtil.utcToKst(row.getRecordFirstCreated()),
            DateUtil.utcToKst(snapshotCreatedAtUtc),
            row.getTitle(),
            row.getSummary(),
            row.getGoodsNo(),
            row.getGoodsDataNo(),
            row.getSourceObjId(),
            isPermitted,
            row.getSourceAvailable() != null && row.getSourceAvailable() != 0);
    }

}
