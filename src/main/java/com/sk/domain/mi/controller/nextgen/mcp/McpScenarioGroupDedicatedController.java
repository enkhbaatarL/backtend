package com.sk.domain.mi.controller.nextgen.mcp;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.request.nextgen.ScenarioGroupListReq;
import com.sk.domain.mi.dto.response.nextgen.CmeBenchmarkGraphRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioDetailViewRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupDetailRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupGraphRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupListRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioIndividualListRes;
import com.sk.domain.mi.dto.response.nextgen.priceinterp.PriceInterpretationSectionRes;
import com.sk.domain.mi.service.nextgen.CmeBenchmarkService;
import com.sk.domain.mi.service.nextgen.PriceInterpretationService;
import com.sk.domain.mi.service.nextgen.ScenarioGroupService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.enums.nextgen.GraphPeriodUnit;
import com.sk.domain.shared.enums.nextgen.IndexName;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Profile("dedicated")
@Tag(name = "MCP Next Gen MI Scenario Group API", description = "MCP client용 Next Gen MI 시나리오 그룹 조회 API입니다.")
@RestController
@RequestMapping("/api/v1/mcp/mi/next-gen/scenario-groups")
@RequiredArgsConstructor
public class McpScenarioGroupDedicatedController {

    private final ScenarioGroupService scenarioGroupService;
    private final PriceInterpretationService priceInterpretationService;
    private final CmeBenchmarkService cmeBenchmarkService;

    @ApiOrder(1)
    @Operation(summary = "시나리오 그룹 목록 조회", description = "기간, 정렬, 검색어, 페이징 조건으로 시나리오 그룹 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<ScenarioGroupListRes> getScenarioGroups(@Valid @ParameterObject ScenarioGroupListReq request) {
        return BaseResponseFactory.create(scenarioGroupService.getScenarioGroups(request));
    }

    @ApiOrder(2)
    @Operation(summary = "개별 시나리오 목록 조회", description = "모든 그룹의 시나리오를 평탄화한 페이징 목록으로 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/scenarios")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<ScenarioIndividualListRes> getScenarioGroupScenarios(
        @Valid @ParameterObject ScenarioGroupListReq request
    ) {
        return BaseResponseFactory.create(scenarioGroupService.getScenarioGroupScenarios(request));
    }

    @ApiOrder(3)
    @Operation(
        summary = "시나리오 그룹 가격전망 그래프 조회",
        description = """
            단일 인덱스와 기간 단위의 MI Base·시나리오 가격전망을 조회합니다.

            시나리오별 가격은 해당 시나리오의 MI 수급모델 실행이 완료된 경우에만 채워집니다.
            가격이 비어 있으면 오류가 아니라 실행 상태 때문이며, scenarioSeries[]의 필드로 판단합니다
            (화면도 같은 기준으로 '미실행' 범례를 표시합니다).
            - status = discarded : 폐기 판정된 시나리오. 응답에는 다른 시나리오와 같은 기준으로 가격이 내려오지만, 화면은 '폐기'로 표시하고 그래프에 그리지 않습니다.
            - executed = false, modelRunning = true : 수급모델 실행 중입니다. 완료 후 다시 조회하면 가격이 채워집니다.
            - executed = false, modelRunning = false : 완료된 수급모델 실행이 없습니다(미실행, 또는 실행했으나 실패). baseCaseValues/userAdjustedValues가 null입니다.
            - executed = true 인데 baseCaseValues/userAdjustedValues가 null : 실행은 완료됐으나 요청한 인덱스(indexName)의 가격 결과가 없습니다.
            - executed = true 이고 값 있음 : 정상. 값 목록은 dates와 같은 순서이며, 빠진 기간은 null입니다.
            executed는 인덱스와 무관하게 '완료된 실행이 있는지'만 뜻하므로, 가격 null 여부와 함께 봐야 합니다.
            """)
    @Parameter(name = "indexName", description = "조회 인덱스 (JKM / TTF / HH). 기본값 JKM")
    @Parameter(name = "periodUnit", description = "기간 단위 (monthly / yearly, 대소문자 무관). 기본값 monthly")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{scenarioGroupId}/graph")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<ScenarioGroupGraphRes> getScenarioGroupGraph(
        @Parameter(description = "시나리오 그룹 ID", example = "1") @PathVariable Long scenarioGroupId,
        @RequestParam(defaultValue = "JKM") IndexName indexName,
        @RequestParam(defaultValue = "monthly") GraphPeriodUnit periodUnit
    ) {
        return BaseResponseFactory.create(
            scenarioGroupService.getScenarioGroupGraph(scenarioGroupId, indexName, periodUnit));
    }

    @ApiOrder(4)
    @Operation(summary = "시나리오 그룹 상세 조회", description = "그룹 요약, 구성 이벤트, 시나리오 가정과 가격전망 해석 section을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{scenarioGroupId}")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<ScenarioGroupDetailRes> getScenarioGroupDetail(
        @Parameter(description = "시나리오 그룹 ID", example = "1") @PathVariable Long scenarioGroupId
    ) {
        return BaseResponseFactory.create(scenarioGroupService.getScenarioGroupDetail(scenarioGroupId));
    }

    @ApiOrder(5)
    @Operation(summary = "개별 시나리오 상세 조회", description = "시나리오 내러티브, 주요 가정, Signpost 정의와 모니터링 이력을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{scenarioGroupId}/scenarios/{scenarioId}")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<ScenarioDetailViewRes> getScenarioDetail(
        @Parameter(description = "시나리오 그룹 ID", example = "1") @PathVariable Long scenarioGroupId,
        @Parameter(description = "시나리오 ID", example = "11") @PathVariable Long scenarioId
    ) {
        return BaseResponseFactory.create(scenarioGroupService.getScenarioDetail(scenarioGroupId, scenarioId));
    }

    @ApiOrder(6)
    @Operation(
        summary = "가격전망 해석 조회",
        description = """
            시나리오 그룹의 시나리오별 가격전망 해석 section을 조회합니다.

            해석은 수급모델 실행이 완료된 시나리오에 대해 사용자가 화면에서 '가격전망 해석 생성'을 요청해야 만들어집니다.
            interpretations가 빈 배열이면 오류가 아니라 아직 생성되지 않은 상태이며, 다음으로 판단합니다.
            - generatable = 그룹 안에 (수급모델 실행 완료) AND (해석 없음 또는 재실행으로 재생성 필요)인 시나리오가 하나라도 있는지 여부.
            - generatable = false, interpretations = [] : 수급모델을 실행한 시나리오가 없어 해석을 만들 수 없습니다(수급모델 미실행).
            - generatable = true, interpretations = [] : 수급모델은 실행됐으나 해석을 아직 생성하지 않았습니다.
            - generatable = true, 일부만 존재 : 해석이 없거나 재실행으로 최신이 아닌 시나리오가 있습니다.
            - interpretations[].status : generating(생성중) / generated(생성완료) / failed(재생성 실패 — 직전 해석이 있을 때만 포함).
            '수급모델 미실행'과 '실행 시나리오 없음'을 정확히 구분하려면 시나리오 그룹 가격전망 그래프 조회 결과의
            scenarioSeries[].executed를 함께 확인합니다(화면도 그래프 결과와 함께 판단합니다).
            """)
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{scenarioGroupId}/price-interpretations")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<PriceInterpretationSectionRes> getPriceInterpretations(
        @Parameter(description = "시나리오 그룹 ID", example = "1") @PathVariable Long scenarioGroupId
    ) {
        return BaseResponseFactory.create(priceInterpretationService.getInterpretations(scenarioGroupId));
    }

    @ApiOrder(7)
    @Operation(
        summary = "CME 벤치마크 기준선 조회",
        description = """
            SK Gas 전용 서버 전용 API입니다. 일반 서버의 응답은 신뢰할 수 없습니다.
            단일 인덱스와 기간 단위의 CME 벤치마크 기준선을 전용 DB에서 조회합니다.
            """)
    @Parameter(name = "indexName", description = "조회 인덱스 (JKM / TTF / HH). 기본값 JKM")
    @Parameter(name = "periodUnit", description = "기간 단위 (monthly / yearly, 대소문자 무관). 기본값 monthly")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/graph/cme-benchmark")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<CmeBenchmarkGraphRes> getScenarioGroupGraphCmeBenchmark(
        @RequestParam(defaultValue = "JKM") IndexName indexName,
        @RequestParam(defaultValue = "monthly") GraphPeriodUnit periodUnit
    ) {
        return BaseResponseFactory.create(cmeBenchmarkService.getCmeBenchmarkGraph(indexName, periodUnit));
    }
}
