package com.sk.domain.mi.dto.request.activitylog;

import com.sk.domain.shared.enums.ActivityLogType;
import com.sk.domain.shared.enums.SignificanceLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ActivityLogReq {

    public record Insert(
            @Schema(description = "이벤트발생일자 (yyyyMMdd)", example = "20250115", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "이벤트발생일자는 필수입니다")
            @Pattern(regexp = "^\\d{8}$", message = "이벤트발생일자는 yyyyMMdd 형식이어야 합니다")
            String eventDate,

            @Schema(description = "이벤트타입", example = "CUST", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull(message = "이벤트타입은 필수입니다")
            ActivityLogType eventType,

            @Schema(description = "업체명", example = "삼성전자", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "업체명은 필수입니다")
            @Size(max = 100, message = "업체명은 100자 이내여야 합니다")
            String accountName,

            @Schema(description = "이벤트상세설명", example = "신규 계약 체결", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "이벤트상세설명은 필수입니다")
            @Size(max = 500, message = "이벤트상세설명은 500자 이내여야 합니다")
            String eventDetailExpression,

            @Schema(description = "중요도", example = "HIGH", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"LOW", "MEDIUM", "HIGH"})
            @NotNull(message = "중요도는 필수입니다")
            SignificanceLevel significanceLevel,

            @Schema(description = "출처", example = "영업팀", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "출처는 필수입니다")
            @Size(max = 100, message = "출처는 100자 이내여야 합니다")
            String sourceName,

            @Schema(description = "소속팀", example = "마케팅본부", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "소속팀은 필수입니다")
            @Size(max = 100, message = "소속팀은 100자 이내여야 합니다")
            String departmentName
    ) {
    }
}
