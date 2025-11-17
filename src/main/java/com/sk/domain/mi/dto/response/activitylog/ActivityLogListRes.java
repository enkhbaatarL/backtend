package com.sk.domain.mi.dto.response.activitylog;

import io.swagger.v3.oas.annotations.media.Schema;

public record ActivityLogListRes(
        @Schema(description = "활동기록 ID", example = "1")
        Long activityLogId,

        @Schema(description = "이벤트발생일자", example = "20250115")
        String eventDate,

        @Schema(description = "이벤트타입명", example = "고객사")
        String eventTypeName,

        @Schema(description = "업체명", example = "삼성전자")
        String accountName,

        @Schema(description = "이벤트상세설명", example = "신규 계약 체결")
        String eventDetailExpression,

        @Schema(description = "중요도", example = "High", allowableValues = {"High", "Medium", "Low"})
        String significanceLevel,

        @Schema(description = "출처", example = "영업팀")
        String sourceName,

        @Schema(description = "작성자명", example = "홍길동")
        String createUserNm,

        @Schema(description = "소속팀", example = "마케팅본부")
        String departmentName
) {
}
