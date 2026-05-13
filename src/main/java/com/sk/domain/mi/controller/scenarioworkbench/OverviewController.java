package com.sk.domain.mi.controller.scenarioworkbench;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.request.scenarioworkbench.OverviewEventListReq;
import com.sk.domain.mi.dto.response.scenarioworkbench.OverviewEventListRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.OverviewPriceForecastRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.OverviewScenarioListRes;
import com.sk.domain.mi.service.scenarioworkbench.OverviewService;
import com.sk.common.annotation.PermissionRule;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.enums.scenarioworkbench.IndexName;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "1. Scenario Workbench Overview API", description = "Scenario Workbench Overview API입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/scenario-workbench/overview")
@RequiredArgsConstructor
public class OverviewController {

    private final OverviewService overviewService;

    @ApiOrder(1)
    @Operation(
        summary = "Overview Event First 목록 조회 API (with pagination)",
        description = """
            Scenario Canvas의 Event First 모드 데이터를 조회합니다. <br>
            이벤트 목록과 각 이벤트의 하위 시나리오를 한 번에 반환합니다. (Eager 로딩)

            <br><br>
            ### 사용 가능한 파라미터
             - period: 조회 기간 / 기본값 SIX_MONTHS / example - period=THREE_MONTHS, period=CUSTOM
             - startDate: 커스텀 조회 시작일 (period=CUSTOM 시 필수) / example - startDate=2025-09-01
             - endDate: 커스텀 조회 종료일 (period=CUSTOM 시 필수) / example - endDate=2026-02-28
             - sort: 정렬 기준 / 기본값 LATEST(최신순) / example - sort=LATEST, sort=OLDEST
             - page: 기본값 1 / 1-based
             - size: 기본값 10

            <br><br>
            ### Events 목록 API와 차이
            - 상태 필터(eventStatus), 카테고리 필터(eventCategoryLv1), 검색(keyword) 없음
            - 이벤트 행마다 하위 시나리오 목록(`scenarios`) 포함 (accordion 펼침 용도)
            - 기본 페이지 사이즈 10건 (Events 목록은 20건)

            <br><br>
            ### events.detail 항목 필드
            - `eventId`: 이벤트 ID
            - `eventName`: 이벤트명
            - `createdAt`: 생성일시 (crt_dtm)
            - `updatedAt`: 수정일시 (mod_dtm)
            - `eventCategoryLv1`: 카테고리 Lv1
            - `indexImpacts`: 인덱스별 영향도 (marketImpactLevel + direction)
            - `scenarios`: 하위 시나리오 목록 (scenarioId, scenarioName, baseCase, indexImpacts, probability)
            """
    )
    @Parameter(name = "period", description = "조회 기간. 기본값 SIX_MONTHS")
    @Parameter(name = "startDate", description = "커스텀 조회 시작일. period=CUSTOM 시 사용")
    @Parameter(name = "endDate", description = "커스텀 조회 종료일. period=CUSTOM 시 사용")
    @Parameter(name = "sort", description = "정렬 기준. 기본값 LATEST")
    @Parameter(name = "page", description = "페이지 번호. 기본값 1")
    @Parameter(name = "size", description = "페이지당 항목 수. 기본값 10")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/events")
    @RequiredPermission(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<OverviewEventListRes> getOverviewEvents(@Valid @ParameterObject OverviewEventListReq request) {
        return BaseResponseFactory.create(overviewService.getOverviewEvents(request));
    }

    @ApiOrder(2)
    @Operation(
        summary = "Overview Scenario First 목록 조회 API (with pagination)",
        description = """
            Scenario Canvas의 Scenario First 모드 데이터를 조회합니다. <br>
            시나리오 목록과 각 시나리오의 관련 이벤트를 한 번에 반환합니다. (Eager 로딩)

            <br><br>
            ### 사용 가능한 파라미터
             - period: 조회 기간 / 기본값 SIX_MONTHS / example - period=THREE_MONTHS, period=CUSTOM
             - startDate: 커스텀 조회 시작일 (period=CUSTOM 시 필수) / example - startDate=2025-09-01
             - endDate: 커스텀 조회 종료일 (period=CUSTOM 시 필수) / example - endDate=2026-02-28
             - sort: 정렬 기준 / 기본값 LATEST(최신순) / example - sort=LATEST, sort=OLDEST
             - page: 기본값 1 / 1-based
             - size: 기본값 10

            <br><br>
            ### Scenarios 목록 API와 차이
            - 상태 필터(scenarioStatus), 검색(keyword) 없음
            - 시나리오 행마다 관련 이벤트 목록(`events`) 포함 (accordion 펼침 용도)
            - 기본 페이지 사이즈 10건

            <br><br>
            ### scenarios.detail 항목 필드
            - `scenarioId`: 시나리오 ID
            - `scenarioName`: 시나리오명
            - `createdAt`: 생성일시 (crt_dtm)
            - `updatedAt`: 수정일시 (mod_dtm)
            - `tags`: 태그 배지
            - `indexImpacts`: 인덱스별 영향도 (marketImpactLevel only, direction 없음)
            - `probability`: 발생 가능성 (0.00 ~ 1.00)
            - `events`: 관련 이벤트 목록 (eventId, eventName, eventCategoryLv1, indexImpacts)
            """
    )
    @Parameter(name = "period", description = "조회 기간. 기본값 SIX_MONTHS")
    @Parameter(name = "startDate", description = "커스텀 조회 시작일. period=CUSTOM 시 사용")
    @Parameter(name = "endDate", description = "커스텀 조회 종료일. period=CUSTOM 시 사용")
    @Parameter(name = "sort", description = "정렬 기준. 기본값 LATEST")
    @Parameter(name = "page", description = "페이지 번호. 기본값 1")
    @Parameter(name = "size", description = "페이지당 항목 수. 기본값 10")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/scenarios")
    @RequiredPermission(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<OverviewScenarioListRes> getOverviewScenarios(@Valid @ParameterObject OverviewEventListReq request) {
        return BaseResponseFactory.create(overviewService.getOverviewScenarios(request));
    }

    @ApiOrder(3)
    @Operation(
        summary = "Overview Price Forecast 차트 데이터 조회 API",
        description = """
            Overview 우측 가격 전망 차트 데이터를 조회합니다. <br>
            인덱스별로 lazy loading하여 차트 데이터를 반환합니다.

            <br><br>
            ### 사용 가능한 파라미터
             - indexName: (필수) 조회할 인덱스명 / example - indexName=JKM, indexName=TTF, indexName=HH
             - eventId: (선택) Event First — 해당 이벤트의 관련 시나리오만 필터 / example - eventId=1
             - scenarioIds: (선택) Scenario First — 현재 페이지에 표시된 시나리오 ID 목록 / example - scenarioIds=1,2,3

            <br><br>
            ### 필터 우선순위
            - `eventId` 지정 시: 해당 이벤트의 관련 시나리오만 반환
            - `scenarioIds` 지정 시: 해당 시나리오들만 반환
            - 둘 다 없으면: 전체 시나리오 반환

            <br><br>
            ### 응답 구조
            - `indexName`: 인덱스명 (JKM / TTF / HH)
            - `currency`: 통화 (USD / EUR)
            - `unit`: 단위 (MMBtu / MWh)
            - `dates`: 월별 날짜 배열 (모든 시나리오 공유)
            - `series`: 시나리오별 가격 라인 (scenarioId, scenarioName, baseCase, values)
            """
    )
    @Parameter(name = "indexName", description = "인덱스명 (필수)", required = true, example = "JKM")
    @Parameter(name = "eventId", description = "이벤트 ID (선택) — Event First 모드에서 해당 이벤트의 관련 시나리오만 필터", example = "1")
    @Parameter(name = "scenarioIds", description = "시나리오 ID 목록 (선택) — Scenario First 모드에서 현재 페이지의 시나리오 필터", example = "1,2,3")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/price-forecasts")
    @RequiredPermission(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<OverviewPriceForecastRes> getOverviewPriceForecasts(
        @RequestParam IndexName indexName,
        @RequestParam(required = false) Long eventId,
        @RequestParam(required = false) List<Long> scenarioIds
    ) {
        return BaseResponseFactory.create(overviewService.getOverviewPriceForecasts(indexName, eventId, scenarioIds));
    }

}
