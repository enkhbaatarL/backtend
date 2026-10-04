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
    @Operation(summary = "시나리오 그룹 가격전망 그래프 조회", description = "단일 인덱스와 기간 단위의 MI Base·시나리오 가격전망을 조회합니다.")
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
    @Operation(summary = "가격전망 해석 조회", description = "시나리오 그룹의 시나리오별 가격전망 해석 section을 조회합니다.")
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
