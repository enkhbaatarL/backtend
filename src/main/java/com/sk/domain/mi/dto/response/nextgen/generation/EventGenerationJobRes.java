package com.sk.domain.mi.dto.response.nextgen.generation;

import com.sk.domain.shared.entity.master.tmd.MiEventGenerationJob;
import com.sk.domain.shared.enums.nextgen.EventGenerationJobStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * [MCP Event Generate] 이벤트 생성 작업 상태. 상세 조회는 SUCCEEDED 일 때 result 를 담고, 목록 조회는 result 를 담지 않는다.
 */
@Schema(description = "이벤트 생성 작업 상태와 결과")
public record EventGenerationJobRes(

    @Schema(description = "이벤트 생성 작업 ID", example = "42")
    Long jobId,

    @Schema(description = """
        작업 상태. RUNNING: 생성 중 / SUCCEEDED: 생성 완료(result 에 결과) / FAILED: 생성 실패(errorCode, errorMessage) / \
        INTERRUPTED: 서버 재기동 등으로 결과를 받지 못함. 이벤트는 저장됐을 수 있으므로 다시 생성하기 전에 이벤트 목록에서 확인합니다.""",
        example = "RUNNING")
    EventGenerationJobStatus status,

    @Schema(description = "생성 요청 문장", example = "최근 인도 LPG 수입 정책 변화로 이벤트를 만들어줘")
    String userQuery,

    @Schema(description = "근거 조회 시작일(YYYY-MM-DD)", example = "2026-09-01")
    String dateFrom,

    @Schema(description = "근거 조회 종료일(YYYY-MM-DD)", example = "2026-10-12")
    String dateTo,

    @Schema(description = "작업 시작 일시", example = "2026-10-16T14:05:00")
    LocalDateTime startedAt,

    @Schema(description = "작업 종료 일시. RUNNING 이면 null", example = "2026-10-16T14:19:30")
    LocalDateTime completedAt,

    @Schema(description = "실패 코드(FAILED, INTERRUPTED). 그 외 null", example = "API-002")
    String errorCode,

    @Schema(description = "실패 메시지(FAILED, INTERRUPTED). 그 외 null")
    String errorMessage,

    @Schema(description = "생성 결과(meta, events). 상세 조회에서 SUCCEEDED 일 때만 있고, 목록 조회에서는 항상 null")
    EventGenerationRes result

) {

    public static EventGenerationJobRes of(MiEventGenerationJob job, EventGenerationRes result) {
        return new EventGenerationJobRes(
            job.getEventGenerationJobId(),
            job.getStatus(),
            job.getUserQuery(),
            job.getDateFrom(),
            job.getDateTo(),
            job.getStartedAt(),
            job.getCompletedAt(),
            job.getErrorCode(),
            job.getErrorMessage(),
            result
        );
    }

}
