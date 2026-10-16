package com.sk.domain.mi.dto.response.nextgen.generation;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * [MCP Event Generate] 이벤트 생성 결과. 이벤트 생성 서버 응답의 data 를 그대로 전달한다 (필드명·설명은 event_generate_v1 스펙 기준).
 * 값의 해석·가공은 하지 않으며 enum 성격의 값도 String 으로 받아 서버가 새 값을 추가해도 실패하지 않게 한다.
 */
@Schema(description = "이벤트 생성 결과(어느 분기를 왜 탔는지, 저장 결과, 생성된 이벤트 목록)")
public record EventGenerationRes(

    @Schema(description = "생성 결과 메타")
    Meta meta,

    @Schema(description = "생성된 이벤트 목록")
    List<GeneratedEvent> events

) {

    @Schema(description = "생성 결과 메타(어느 분기를 왜 탔는지, 저장 결과, 처리 경고)")
    public record Meta(

        @Schema(description = "근거 수집 분기. web: 웹 검색 / existing_event: 기존 이벤트 검색 / both: 두 검색을 병렬 실행",
            allowableValues = {"web", "existing_event", "both"})
        String route,

        @Schema(description = "route 선택 근거")
        String routeReason,

        @Schema(description = "실제 저장된 이벤트 수")
        Integer persistedCount,

        @Schema(description = "검색 실패·분류 실패·저장 여부 등 처리 경고")
        List<String> warnings

    ) {}

    @Schema(description = "생성된 이벤트 1건(저장 값) + 기존 이벤트 편집 정보·참고 근거 + 저장 ID")
    public record GeneratedEvent(

        @Schema(description = "한 줄 요약")
        String oneLineSummary,

        @Schema(description = "상세 분석")
        String detailAnalysis,

        @Schema(description = "핵심 요점 목록")
        List<String> keyBullets,

        @Schema(description = "태그 목록")
        List<String> tags,

        @Schema(description = "이벤트 발생 일시")
        String occurredAt,

        @Schema(description = "이벤트 유형 L2 ID")
        Long eventTypeL2Id,

        @Schema(description = "기존 이벤트 편집 방식(existing_event 분기만, 그 외 null). merge: 병합 / modify: 수치 수정",
            allowableValues = {"merge", "modify"})
        String operation,

        @Schema(description = "병합·수정의 원본 기존 이벤트 ID 목록")
        List<Long> sourceMiEventIds,

        @Schema(description = "이 이벤트를 이렇게 생성한 이유")
        String generationRationale,

        @Schema(description = "생성 시 참고한 근거 목록")
        List<Evidence> evidences,

        @Schema(description = "저장된 이벤트 ID(eventId). 저장되지 않았으면 null. 이벤트 상세 조회에 이 값을 쓸 수 있습니다.")
        Long miEventId

    ) {}

    @Schema(description = "초안이 참고한 근거 1건")
    public record Evidence(

        @Schema(description = "근거 키(예: T1-W1, T2-E3)")
        String evidenceKey,

        @Schema(description = "근거 출처 종류. web: 웹 검색 / existing_event: 기존 이벤트", allowableValues = {"web", "existing_event"})
        String origin,

        @Schema(description = "근거 제목")
        String title,

        @Schema(description = "출처명")
        String sourceName,

        @Schema(description = "게시 일시")
        String publishedAt,

        @Schema(description = "근거 URL")
        String url,

        @Schema(description = "web 근거를 찾은 검색 provider. existing_event면 빈 값")
        String provider,

        @Schema(description = "existing_event면 참고한 기존 이벤트 ID")
        Long refId,

        @Schema(description = "이 근거에서 가져온 사실·수치·주장")
        String referencedContent,

        @Schema(description = "이 근거의 반영 위치·방식")
        String usage

    ) {}

}
