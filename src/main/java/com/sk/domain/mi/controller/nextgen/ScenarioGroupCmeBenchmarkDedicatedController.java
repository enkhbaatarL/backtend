package com.sk.domain.mi.controller.nextgen;

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
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * [Dedicated] 시나리오 그룹 그래프 CME 벤치마크 기준선 API.
 *
 * <p>CME 데이터는 고객사 전용 DB({@code midb-skgas})에 적재되어 일반 서버에서 접근할 수 없으므로,
 * {@code dedicated} 프로필에서만 동작한다. MI Base/시나리오 시리즈는 기존 그래프 API를 그대로 사용하고,
 * 프론트는 본 엔드포인트(customBaseUrl)를 호출해 CME 기준선을 차트에 머지한다.
 */
@Profile("dedicated")
@Tag(name = "02. Next Gen MI Scenario Group API (Dedicated)", description = "Next Gen MI Scenario Group 전용 API입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/next-gen/scenario-groups/graph")
@RequiredArgsConstructor
public class ScenarioGroupCmeBenchmarkDedicatedController {

    private final CmeBenchmarkService cmeBenchmarkService;

    @ApiOrder(1)
    @Operation(
        summary = "Scenario Group 그래프 CME 벤치마크 기준선 조회 API",
        description = """
            그래프 뷰의 CME 벤치마크 기준선 데이터입니다. (전용 DB `midb-skgas` 조회) <br>
            `indexName`(기본 JKM) × `periodUnit`(기본 monthly)으로 **단일 인덱스 × 단일 기간단위**만 반환합니다. <br>
            MI Base/시나리오 시리즈는 기존 그래프 API(`/{scenarioGroupId}/graph`)를 사용하세요. <br>

            <br><br>
            ### 응답 필드
            - `periodUnit` / `periodUnitLabel`: 기간 단위 (monthly:월별 / yearly:연도별)
            - `dates`: x축 기간 목록 (월별 `yyyy-MM` / 연도별 `yyyy`)
            - `baseSeries`: 기준선 — `CME` 제공 (seriesId / seriesName / values, dates와 동일 순서)
              - 연도별은 해당 연 월값의 평균
            - 표출 기간: 현재 연도 ~ 2030년 (Future, index_div=F 기준)
            """
    )
    @Parameter(name = "indexName", description = "조회 인덱스 (JKM / TTF / HH). 기본값 JKM")
    @Parameter(name = "periodUnit", description = "기간 단위 (`monthly` / `yearly`, 대소문자 무관). 기본값 `monthly`")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/cme-benchmark")
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
