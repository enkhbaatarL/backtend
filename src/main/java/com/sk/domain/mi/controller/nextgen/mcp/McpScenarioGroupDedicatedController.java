package com.sk.domain.mi.controller.nextgen.mcp;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.response.nextgen.CmeBenchmarkGraphRes;
import com.sk.domain.mi.service.nextgen.CmeBenchmarkService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.enums.nextgen.GraphPeriodUnit;
import com.sk.domain.shared.enums.nextgen.IndexName;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(
    name = "MCP Next Gen MI Scenario Group Dedicated API",
    description = "SK Gas 전용 서버에서만 신뢰할 수 있는 MCP CME 벤치마크 조회 API입니다.")
@RestController
@RequestMapping("/api/v1/mcp/mi/next-gen/scenario-groups")
@RequiredArgsConstructor
public class McpScenarioGroupDedicatedController {

    private final CmeBenchmarkService cmeBenchmarkService;

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
