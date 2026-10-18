package com.sk.domain.mi.dto.response.nextgen.generation;

import com.sk.domain.shared.entity.master.tmd.MiEventGenerationJob;
import com.sk.domain.shared.enums.nextgen.EventGenerationJobStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * [MCP Event Generate] 이벤트 생성 접수 결과. 결과는 jobId 로 작업 조회 API 에서 받는다.
 */
@Schema(description = "이벤트 생성 접수 결과")
public record EventGenerationAcceptRes(

    @Schema(description = "이벤트 생성 작업 ID. 이벤트 생성 작업 조회에 이 값을 씁니다.", example = "42")
    Long jobId,

    @Schema(description = "작업 상태. 접수 직후에는 항상 RUNNING 입니다.", example = "RUNNING")
    EventGenerationJobStatus status,

    @Schema(description = "작업 시작 일시", example = "2026-10-16T14:05:00")
    LocalDateTime startedAt,

    @Schema(description = "같은 사용자가 같은 입력으로 이미 진행 중인 작업이 있어 새로 만들지 않고 그 작업을 돌려줬는지 여부", example = "false")
    boolean reused

) {

    public static EventGenerationAcceptRes of(MiEventGenerationJob job, boolean reused) {
        return new EventGenerationAcceptRes(job.getEventGenerationJobId(), job.getStatus(), job.getStartedAt(), reused);
    }

}
