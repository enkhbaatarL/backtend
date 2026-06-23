package com.sk.domain.mi.controller.nextgen;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.utils.DateUtil;
import com.sk.domain.mi.dto.response.nextgen.CmeBenchmarkGraphRes;
import com.sk.domain.mi.service.nextgen.CmeBenchmarkService;
import com.sk.domain.mi.service.nextgen.ScenarioGroupGraphExportDedicatedService;
import com.sk.domain.shared.service.FileDownloadEncryptor;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.enums.nextgen.GraphPeriodUnit;
import com.sk.domain.shared.enums.nextgen.IndexName;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.ByteArrayInputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * [Dedicated] 시나리오 그룹 API.
 *
 * <p>CME 데이터는 고객사 전용 DB({@code midb-skgas})에 적재되어 일반 서버에서 접근할 수 없으므로,
 * {@code dedicated} 프로필에서만 동작한다. MI Base/시나리오 시리즈는 기존 그래프 API를 그대로 사용하고,
 * 프론트는 본 엔드포인트(customBaseUrl)를 호출해 CME 기준선을 차트에 머지한다.
 */
@Profile(value = {"dedicated","testdb"})
@Tag(name = "02. Next Gen MI Scenario Group API (Dedicated)", description = "Next Gen MI Scenario Group 전용 API입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/next-gen/scenario-groups")
@RequiredArgsConstructor
public class ScenarioGroupDedicatedController {

    private final CmeBenchmarkService cmeBenchmarkService;
    private final ScenarioGroupGraphExportDedicatedService scenarioGroupGraphExportService;
    private final FileDownloadEncryptor fileDownloadEncryptor;

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
              - 모든 가격 값은 **소수 2자리 반올림(HALF_UP)**
              - 연도별은 해당 연 월값의 평균
            - 표출 기간: 현재 연도 ~ 2030년 (Future, index_div=F 기준)
            """
    )
    @Parameter(name = "indexName", description = "조회 인덱스 (JKM / TTF / HH). 기본값 JKM")
    @Parameter(name = "periodUnit", description = "기간 단위 (`monthly` / `yearly`, 대소문자 무관). 기본값 `monthly`")
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

    @ApiOrder(2)
    @Operation(
        summary = "Scenario Group 그래프 엑셀 다운로드 API (Dedicated)",
        description = """
            그래프 데이터를 엑셀(.xlsx)로 다운로드 합니다. **전 인덱스(JKM/TTF/HH)를 인덱스별 시트**로 묶어 한 파일로 내려줍니다. <br>
            MI Base/시나리오 시리즈(공통 DB) + **CME 기준선(전용 DB)을 한 파일에 합본**합니다. <br>
            기간 단위는 `periodUnit`(기본 monthly) 하나만 적용되며, 시나리오별로 베이스케이스/사용자조정 2행씩 내려갑니다. <br>
            모든 가격 값은 **소수 2자리 반올림(HALF_UP)** 으로 표기됩니다. <br>

            <br>
            MI와 CME는 산출 기준이 달라 기간 범위가 어긋날 수 있으며, 정합하지 않고 각자 자기 기간을 표출합니다(겹치지 않는 칸은 빈 칸).
            """
    )
    @Parameter(name = "scenarioGroupId", description = "시나리오 그룹 ID", example = "1")
    @Parameter(name = "periodUnit", description = "기간 단위 (`monthly` / `yearly`, 대소문자 무관). 기본값 `monthly`")
    @ApiResponse(responseCode = "200", description = "다운로드 성공")
    @GetMapping("/{scenarioGroupId}/graph/export")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public ResponseEntity<InputStreamResource> exportScenarioGroupGraph(
        @PathVariable Long scenarioGroupId,
        @RequestParam(defaultValue = "monthly") GraphPeriodUnit periodUnit
    ) {
        String filename = "scenario-group-%s-graph-%s-%s.xlsx".formatted(scenarioGroupId, periodUnit.getCode(), DateUtil.currentYmd());

        byte[] plain = scenarioGroupGraphExportService.exportScenarioGroupGraph(scenarioGroupId, periodUnit).readAllBytes();
        byte[] data = fileDownloadEncryptor.encryptForDownloadIfRequired(filename, plain);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(data.length);

        return new ResponseEntity<>(new InputStreamResource(new ByteArrayInputStream(data)), headers, HttpStatus.OK);
    }

}
