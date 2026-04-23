package com.sk.domain.mi.controller.scenarioworkbench;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.request.scenarioworkbench.EventListReq;
import com.sk.domain.mi.dto.response.scenarioworkbench.EventDetailRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.EventInstitutionOutlookRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.EventListRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.EventRelatedRecordRes;
import com.sk.domain.mi.service.scenarioworkbench.EventService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "1. Scenario Workbench Event API", description = "Scenario Workbench Event API입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/scenario-workbench/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @ApiOrder(1)
    @Operation(
        summary = "Event 목록 조회 API (with pagination)",
        description = """
            Scenario Workbench의 Event 목록을 필터, 정렬, 페이징 조건으로 조회합니다. <br>

            <br><br>
            ### 사용 가능한 파라미터
             - page: 기본값 1 / 1-based
             - size: 기본값 20
             - sort: 정렬 기준 / 기본값 LATEST(최신순) / example - sort=LATEST, sort=OLDEST
             - eventStatus: 이벤트 상태 필터 (복수 가능) / example - eventStatus=NEW&eventStatus=UPDATED
             - eventCategoryLv1: 이벤트 유형 필터 (복수 가능) / example - eventCategoryLv1=GEOPOLITICAL_RISK&eventCategoryLv1=WEATHER_CLIMATE
             - period: 조회 기간 / 기본값 SIX_MONTHS / example - period=THREE_MONTHS, period=CUSTOM
             - startDate: 커스텀 조회 시작일 (period=CUSTOM 시 필수) / example - startDate=2025-09-01
             - endDate: 커스텀 조회 종료일 (period=CUSTOM 시 필수) / example - endDate=2026-02-28
             - keyword: 이벤트 제목 검색 키워드 / example - keyword=호르무즈

            <br><br>
            ### summaryNotice 필드
            - `summaryNotice`는 현재 조회 기간 내 신규/업데이트 이벤트 수를 요약한 알림 정보입니다.
            - `newEventCount`: 신규 이벤트 수
            - `updatedEventCount`: 업데이트된 이벤트 수

            <br><br>
            ### appliedFilter 필드
            - `appliedFilter`는 현재 요청에 적용된 필터 조건을 그대로 반환합니다.

            <br><br>
            ### events.content 항목 필드
            - `eventId`: 이벤트 고유 식별자
            - `title`: 이벤트 제목
            - `eventStatus`: 이벤트 상태 (NEW / UPDATED / CLOSED)
            - `eventCategoryLv1`: 이벤트 유형 Lv1 (GEOPOLITICAL_RISK, WEATHER_CLIMATE, INFRASTRUCTURE_FACILITY, SUPPLY_DEMAND_BALANCE, LOGISTICS_SHIPPING, ENERGY_MIX_TRANSITION, MACROECONOMY_POLICY, MARKET_TRADING_STRUCTURE)
            - `occurredAt`: 이벤트 발생/인지 시각
            - `updatedAt`: 마지막 업데이트 시각
            - `indexImpacts`: 인덱스별 영향도 (JKM, TTF, HH 각각의 impactLevel + priceDirection)
            """
    )
    @Parameter(name = "eventStatus", description = "이벤트 상태. 복수 선택 가능")
    @Parameter(name = "eventCategoryLv1", description = "이벤트 유형 Lv1. 복수 선택 가능")
    @Parameter(name = "period", description = "조회 기간. 기본값 SIX_MONTHS")
    @Parameter(name = "startDate", description = "커스텀 조회 시작일. period=CUSTOM 시 사용")
    @Parameter(name = "endDate", description = "커스텀 조회 종료일. period=CUSTOM 시 사용")
    @Parameter(name = "sort", description = "정렬 기준. 기본값 LATEST")
    @Parameter(name = "keyword", description = "검색 키워드")
    @Parameter(name = "page", description = "페이지 번호. 기본값 1")
    @Parameter(name = "size", description = "페이지당 항목 수. 기본값 20")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    @RequiredPermission(
        roles = {Role.ADMIN, Role.USER},
        condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY
    )
    public BaseResponse<EventListRes> getEvents(@Valid @ParameterObject EventListReq request) {
        return BaseResponseFactory.create(eventService.getEvents(request));
    }

    @ApiOrder(2)
    @Operation(
        summary = "Event 상세 조회 API",
        description = """
            Event 상세 페이지의 핵심 데이터를 조회합니다. <br>
            `영향도 분석`, `AI Insights`, `Fundamental Deep-dive` 정보가 포함됩니다. <br>
            (Records와 기관 전망은 별도 페이징 API를 사용합니다.)

            <br><br>
            ### impactAnalysis 필드
            이벤트가 LNG 시장 주요 인덱스(JKM, TTF, Henry Hub)에 미치는 영향을 분석한 결과입니다.
            - `indexImpacts`: 인덱스별 영향도 요약 (indexName, impactLevel, priceDirection)
            - `analysisText`: AI가 생성한 영향도 분석 본문
            - `categoryLv1` / `categoryLv2`: 이벤트 유형 분류 (Lv1: 8종, Lv2: 20종)
            - `sources`: 분석에 사용된 출처 목록

            <br><br>
            ### aiInsights 필드
            AI 에이전트가 생성한 이벤트 핵심 요약 및 상세 분석입니다.
            - `keySummaries`: 핵심 요약 포인트 (3~5개 bullet)
            - `detailedAnalysis`: 상세 분석 본문

            <br><br>
            ### fundamentalDeepdive 필드
            이벤트 관련 시장 펀더멘탈 데이터와 Driver Tree Scoreboard입니다.
            - `metrics`: 정량 지표 (metricName, currentValue, interpretation, source)
            - `indicators`: 시장 시그널 지표 (indicatorName, currentValue, implication, source)
            """
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{eventId}")
    @RequiredPermission(
        roles = {Role.ADMIN, Role.USER},
        condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY
    )
    public BaseResponse<EventDetailRes> getEventDetail(
        @Parameter(description = "이벤트 ID", example = "ev_8f3a91c2b7d0") @PathVariable String eventId
    ) {
        return BaseResponseFactory.create(eventService.getEventDetail(eventId));
    }

    @ApiOrder(3)
    @Operation(
        summary = "Event Records 조회 API (with pagination)",
        description = """
            Event에 매핑된 Record(뉴스/리포트) 목록을 페이징 조회합니다. <br>

            <br><br>
            ### 사용 가능한 파라미터
             - eventId: 이벤트 ID (Path) / example - ev_8f3a91c2b7d0
             - page: 기본값 1 / 1-based
             - size: 기본값 10

            <br><br>
            ### content 항목 필드
            - `publishedAt`: Record 발행 일시
            - `title`: Record 제목
            - `summary`: Record 요약 (1~2줄)
            - `source`: 출처 (Reuters, Platts, Bloomberg 등)
            """
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{eventId}/records")
    @RequiredPermission(
        roles = {Role.ADMIN, Role.USER},
        condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY
    )
    public BaseResponse<PaginationRes<EventRelatedRecordRes>> getEventRecords(
        @Parameter(description = "이벤트 ID", example = "ev_8f3a91c2b7d0") @PathVariable String eventId,
        @Parameter(description = "페이지 번호", example = "1") @RequestParam(defaultValue = "1") @Min(1) int page,
        @Parameter(description = "페이지당 항목 수", example = "10") @RequestParam(defaultValue = "10") @Min(1) int size
    ) {
        return BaseResponseFactory.create(eventService.getEventRecords(eventId, page, size));
    }

    @ApiOrder(4)
    @Operation(
        summary = "Event 기관 전망 조회 API (with pagination)",
        description = """
            Event 관련 기관별 시장 전망 정보를 페이징 조회합니다. <br>

            <br><br>
            ### 사용 가능한 파라미터
             - eventId: 이벤트 ID (Path) / example - ev_8f3a91c2b7d0
             - page: 기본값 1 / 1-based
             - size: 기본값 10

            <br><br>
            ### content 항목 필드
            - `publishedAt`: 전망 발행 일시
            - `institutionName`: 기관명 (Goldman Sachs, IEA, Wood Mackenzie 등)
            - `outlookSummary`: 전망 요약 (1~2줄)
            """
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{eventId}/outlooks")
    @RequiredPermission(
        roles = {Role.ADMIN, Role.USER},
        condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY
    )
    public BaseResponse<PaginationRes<EventInstitutionOutlookRes>> getEventOutlooks(
        @Parameter(description = "이벤트 ID", example = "ev_8f3a91c2b7d0") @PathVariable String eventId,
        @Parameter(description = "페이지 번호", example = "1") @RequestParam(defaultValue = "1") @Min(1) int page,
        @Parameter(description = "페이지당 항목 수", example = "10") @RequestParam(defaultValue = "10") @Min(1) int size
    ) {
        return BaseResponseFactory.create(eventService.getEventOutlooks(eventId, page, size));
    }

}
