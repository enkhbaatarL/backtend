package com.sk.domain.mi.controller.scenarioworkbench;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.request.scenarioworkbench.ScenarioListReq;
import com.sk.domain.mi.dto.response.scenarioworkbench.ScenarioDetailRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.ScenarioListRes;
import com.sk.domain.mi.service.scenarioworkbench.ScenarioService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "2. Scenario Workbench Scenario API", description = "Scenario Workbench Scenario API입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/scenario-workbench/scenarios")
@RequiredArgsConstructor
public class ScenarioController {

    private final ScenarioService scenarioService;

    @ApiOrder(1)
    @Operation(
        summary = "Scenario 목록 조회 API (with pagination)",
        description = """
            Scenario Workbench의 Scenario 목록을 필터, 정렬, 페이징 조건으로 조회합니다. <br>

            <br><br>
            ### 사용 가능한 파라미터
             - scenarioStatus: 시나리오 상태 / 기본값 ALL / example - scenarioStatus=NEW, scenarioStatus=UPDATED
             - sort: 정렬 기준 / 기본값 LATEST(최신순) / example - sort=LATEST, sort=OLDEST
             - period: 조회 기간 / 기본값 SIX_MONTHS / example - period=THREE_MONTHS, period=CUSTOM
             - startDate: 커스텀 조회 시작일 (period=CUSTOM 시 필수) / example - startDate=2025-09-01
             - endDate: 커스텀 조회 종료일 (period=CUSTOM 시 필수) / example - endDate=2026-02-28
             - keyword: 시나리오명 검색 키워드 / example - keyword=호르무즈
             - page: 기본값 1 / 1-based
             - size: 기본값 20

            <br><br>
            ### 시나리오 상태 (scenarioStatus)
            - `ALL`: 전체 시나리오
            - `NEW`: 신규 시나리오 (crt_dtm == mod_dtm, 생성 후 갱신 없음)
            - `UPDATED`: 갱신 시나리오 (crt_dtm != mod_dtm, 1회 이상 갱신됨)

            <br><br>
            ### scenarios.content 항목 필드
            - `scenarioId`: 시나리오 고유 식별자
            - `scenarioName`: 시나리오명
            - `createdAt`: 생성일시
            - `updatedAt`: 수정일시
            - `indexImpacts`: 인덱스별 영향도 (JKM, TTF, HH)
            - `probability`: 발생 가능성 (0.00 ~ 1.00)
            """
    )
    @Parameter(name = "scenarioStatus", description = "시나리오 상태 (ALL / NEW / UPDATED). 기본값 ALL")
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
    public BaseResponse<ScenarioListRes> getScenarios(@Valid @ParameterObject ScenarioListReq request) {
        return BaseResponseFactory.create(scenarioService.getScenarios(request));
    }

    @ApiOrder(2)
    @Operation(
        summary = "Scenario 상세 조회 API",
        description = """
            Scenario 상세 페이지의 전체 데이터를 조회합니다. <br>
            4개 탭의 데이터가 모두 포함됩니다. <br>

            <br><br>
            ### summary (시나리오 요약 - Tab 1)
            - `indexOutlooks`: 인덱스별 요약 (영향도, 가격방향, 가격 흐름, 경향 전망)
            - `priceForecast`: 가격 예측 라인차트 데이터 (월별 시계열)

            <br><br>
            ### assumptions (시나리오 주요 가정 - Tab 2)
            - 가정 항목 카드 (label, value, supportingText, direction)

            <br><br>
            ### signposts (Signpost 모니터링 - Tab 3)
            - 핵심이벤트, 현재 상태 요약, 시장 영향 해석, 관찰 포인트(Trigger), 출처

            <br><br>
            ### relatedEventScenario (관련 이벤트 및 시나리오 - Tab 4)
            - `event`: 관련 이벤트 (1건) - 이벤트명, JKM/TTF/HH 영향도
            - `scenarios`: 관련 시나리오 - 시나리오명, JKM/TTF/HH 영향도, 가능성, 베이스케이스 여부 (BC 배지)
            """
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{scenarioId}")
    @RequiredPermission(
        roles = {Role.ADMIN, Role.USER},
        condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY
    )
    public BaseResponse<ScenarioDetailRes> getScenarioDetail(
        @Parameter(description = "시나리오 ID", example = "1") @PathVariable Long scenarioId
    ) {
        return BaseResponseFactory.create(scenarioService.getScenarioDetail(scenarioId));
    }

}
