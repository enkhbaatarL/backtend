package com.sk.domain.mi.controller.nextgen;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.enums.ResultCode;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.request.nextgen.ScenarioGroupListReq;
import com.sk.domain.mi.dto.request.nextgen.ScenarioStatusUpdateReq;
import com.sk.domain.mi.dto.response.nextgen.ScenarioDetailViewRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupDetailRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupGraphRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupListRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioIndividualListRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioStatusRes;
import com.sk.domain.mi.service.nextgen.ScenarioGroupService;
import com.sk.domain.mi.service.nextgen.mock.ScenarioDesignMockService;
import com.sk.domain.mi.service.nextgen.mock.ScenarioGroupMockService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.enums.nextgen.GraphPeriodUnit;
import com.sk.domain.shared.enums.nextgen.IndexName;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.ByteArrayInputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Profile("!dedicated")
@Tag(name = "02. Next Gen MI Scenario Group API", description = "Next Gen MI Scenario Group API입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/next-gen/scenario-groups")
@RequiredArgsConstructor
public class ScenarioGroupController {

    private final ScenarioGroupService scenarioGroupService;
    private final ScenarioGroupMockService scenarioGroupMockService;
    private final ScenarioDesignMockService scenarioDesignMockService;

    @ApiOrder(1)
    @Operation(
        summary = "Scenario Group 목록 조회 API (그룹, with pagination)",
        description = """
            Scenario Group List 화면의 '그룹' 모드 목록입니다. <br>
            한 행이 시나리오 그룹이며, 펼치면 dimensionColumns(동적 표 헤더) + scenarios(표 본문)로 표 뷰를 구성합니다. <br>

            <br><br>
            ### 사용 가능한 파라미터
             - sort: 정렬 기준 / 기본값 latest(최신순) / 그룹 수정일시 기준 정렬 / example - sort=latest, sort=oldest
             - startDate / endDate: 조회 기간 (그룹 수정일시 기준)
             - keyword: 시나리오 그룹명 검색 키워드 / example - keyword=카타르
             - page: 기본값 1 / 1-based
             - size: 기본값 20

            <br><br>
            ### content 항목 필드
            - `scenarioGroupId`: 시나리오 그룹 식별자
            - `groupName`: 시나리오 그룹명
            - `createdAt`: 생성일시
            - `updatedAt`: 수정일시 (최신순 정렬·기간 필터 기준)
            - `summary`: 시나리오 그룹 요약
            - `scenarioCount`: 시나리오 수
            - `dimensionColumns`: 그룹별 동적 표 헤더 (key, label)
            - `scenarios`: 그룹 내 시나리오 표 본문 (scenarioId, scenarioName, baseCase, dimensions, probability, adjustedProbability, status, statusLabel[대기/실현/폐기])
            """
    )
    @Parameter(name = "sort", description = "정렬 기준 (`latest` / `oldest`, 대소문자 무관). 기본값 `latest`")
    @Parameter(name = "startDate", description = "조회 시작일 (yyyy-MM-dd, 그룹 수정일시 기준)")
    @Parameter(name = "endDate", description = "조회 종료일 (yyyy-MM-dd, 그룹 수정일시 기준)")
    @Parameter(name = "keyword", description = "시나리오 그룹명 검색 키워드")
    @Parameter(name = "page", description = "페이지 번호 (1-based). 기본값 1")
    @Parameter(name = "size", description = "페이지당 항목 수. 기본값 20")
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
    @Operation(
        summary = "개별 Scenario 목록 조회 API (개별, with pagination)",
        description = """
            Scenario Group List 화면의 '개별' 모드 목록입니다. <br>
            모든 그룹의 시나리오를 평탄화한 평면 목록으로, 각 행은 부모 그룹 참조(group) + 차원값(dimensions) + 발생 확률(probability)을 가집니다. <br>

            <br><br>
            ### 사용 가능한 파라미터
             - sort: 정렬 기준 (latest / oldest) / 기본값 latest / 시나리오 수정일시 기준 정렬
             - startDate / endDate: 조회 기간 (시나리오 수정일시 기준)
             - keyword: 시나리오명 검색 키워드
             - page / size: 페이징

            <br><br>
            ### content 항목 필드
            - `scenarioId`, `scenarioName`, `baseCase`
            - `createdAt`: 시나리오 생성일시
            - `updatedAt`: 시나리오 수정일시 (최신순 정렬·기간 필터 기준)
            - `group`: 부모 그룹 (scenarioGroupId, groupName, createdAt)
            - `dimensions`: 차원값 목록
            - `probability`: 시나리오 발생 확률 (0~100 정수, 원본)
            - `adjustedProbability`: 재분배 확률 — 그룹 내 폐기 제외 비례 재분배(활성 합 100, 폐기=0). 조회 시점 계산값(비영속)
            - `status` / `statusLabel`: 판정 상태 (pending/realized/discarded — 대기/실현/폐기)
            """
    )
    @Parameter(name = "sort", description = "정렬 기준 (`latest` / `oldest`, 대소문자 무관). 기본값 `latest`")
    @Parameter(name = "startDate", description = "조회 시작일 (yyyy-MM-dd, 시나리오 수정일시 기준)")
    @Parameter(name = "endDate", description = "조회 종료일 (yyyy-MM-dd, 시나리오 수정일시 기준)")
    @Parameter(name = "keyword", description = "시나리오명 검색 키워드")
    @Parameter(name = "page", description = "페이지 번호 (1-based). 기본값 1")
    @Parameter(name = "size", description = "페이지당 항목 수. 기본값 20")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/scenarios")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<ScenarioIndividualListRes> getScenarioGroupScenarios(@Valid @ParameterObject ScenarioGroupListReq request) {
        return BaseResponseFactory.create(scenarioGroupService.getScenarioGroupScenarios(request));
    }

    @ApiOrder(3)
    @Operation(
        summary = "Scenario Group 그래프 데이터 조회 API",
        description = """
            시나리오 그룹 펼침 시 '그래프' 뷰의 데이터입니다. <br>
            `indexName`(기본 JKM) × `periodUnit`(기본 monthly)으로 **단일 인덱스 × 단일 기간단위**만 반환합니다. <br>

            <br><br>
            ### 응답 필드
            - `periodUnit` / `periodUnitLabel`: 기간 단위 (monthly:월별 / yearly:연도별)
            - `dates`: x축 기간 목록 (월별 `yyyy-MM` / 연도별 `yyyy`)
            - `baseSeries`: 기준선 — `MI Base`(exec_type_cd=MI_BASE 최신 케이스 가격) 제공
              - `seriesId` / `seriesName` / `values`(dates와 동일 순서, 빠진 구간 null)
            - `scenarioSeries`: 시나리오별 시리즈
              - `scenarioId` / `scenarioName` / `baseCase`
              - `status` / `statusLabel`: 판정 상태 (pending/realized/discarded — 폐기여부)
              - `executed`: 수급모델 실행 완료 여부 (최신 완료 실행의 가격 결과 존재)
              - `modelRunning`: 수급모델 실행중 여부 (RUNNING 케이스 존재). executed와 독립 — 재실행 중엔 둘 다 true
              - `baseCaseValues` / `userAdjustedValues`: 베이스케이스/사용자조정 가격 (최신 완료 결과, 재실행 중에도 직전 결과 유지)
            """
    )
    @Parameter(name = "indexName", description = "조회 인덱스 (JKM / TTF / HH). 기본값 JKM")
    @Parameter(name = "periodUnit", description = "기간 단위 (`monthly` / `yearly`, 대소문자 무관). 기본값 `monthly`")
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
        return BaseResponseFactory.create(scenarioGroupService.getScenarioGroupGraph(scenarioGroupId, indexName, periodUnit));
    }

    @ApiOrder(4)
    @Operation(
        summary = "Scenario Group 그래프 엑셀 다운로드 API",
        description = """
            그래프 데이터를 엑셀(.xlsx)로 다운로드 합니다. <br>
            **전 인덱스(JKM/TTF/HH)를 인덱스별 시트**로 묶어 한 파일로 내려줍니다. <br>
            기간 단위는 `periodUnit`(기본 monthly) 하나만 적용되며, 시나리오별로 베이스케이스/사용자조정 2행씩 내려갑니다.
            """
    )
    @Parameter(name = "periodUnit", description = "기간 단위 (`monthly` / `yearly`, 대소문자 무관). 기본값 `monthly`")
    @ApiResponse(responseCode = "200", description = "다운로드 성공")
    @GetMapping("/{scenarioGroupId}/graph/export")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public ResponseEntity<InputStreamResource> exportScenarioGroupGraph(
        @Parameter(description = "시나리오 그룹 ID", example = "1") @PathVariable Long scenarioGroupId,
        @RequestParam(defaultValue = "monthly") GraphPeriodUnit periodUnit
    ) {
        ByteArrayInputStream excel = scenarioGroupService.exportScenarioGroupGraph(scenarioGroupId, periodUnit);

        String filename = "scenario-group-%s-graph-%s.xlsx".formatted(scenarioGroupId, periodUnit.getCode());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());

        return new ResponseEntity<>(new InputStreamResource(excel), headers, HttpStatus.OK);
    }

    @ApiOrder(5)
    @Operation(
        summary = "Scenario Group 상세 조회 API",
        description = """
            시나리오 그룹 상세 페이지 데이터입니다. <br>
            가격 전망 탭은 그래프 API(`/{scenarioGroupId}/graph`), 개별 시나리오 상세는 scenario 상세 API (`/{scenarioGroupId}/scenarios/{scenarioId}`)를 사용하세요. <br>

            <br><br>
            ### 상위(top-level) 필드
            - `scenarioGroupId`, `groupName`, `createdAt`, `updatedAt`
            - `baseEvent`: 기반 이벤트 (eventId, eventName, eventTypeLv1, eventTypeLv1Label, updatedAt)
            - `summary`: 시나리오 그룹 설명 — 요약 탭
            - `scenarioCount`: 시나리오 수
            - `dimensionColumns`: 가정 표 동적 헤더 (key, label)
            - `scenarios`: 가정 및 실행 가능성 표 행 (dimensions, probability, adjustedProbability, status, statusLabel[대기/실현/폐기])
            - `scenarios[].adjustedProbability`: 재분배 확률 — 폐기 제외 비례 재분배(활성 합 100, 폐기=0). 조회 시점 계산값(비영속)
            """
    )
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

    @ApiOrder(6)
    @Operation(
        summary = "Scenario 상세 조회 API",
        description = """
            그룹 상세의 '시나리오 상세' 탭에서 선택한 개별 시나리오의 상세 데이터입니다. <br>

            <br><br>
            ### 상위(top-level) 필드
            - `scenarioId`, `scenarioGroupId`, `scenarioName`, `baseCase`
            - `createdAt` / `updatedAt`: 시나리오 생성/수정일시
            - `narrative`: 시나리오 내러티브
            - `probability`: 시나리오 발생 확률 (0~100, 원본)
            - `adjustedProbability`: 재분배 확률 — 그룹 내 폐기 제외 비례 재분배(활성 합 100, 폐기=0). 조회 시점 계산값(비영속)
            - `dimensions`: 주요 가정 (key, label, value, valueLabel)
            - `signpostDefinitions`: Signpost 정의 (signpostId, label, direction[sup], directionLabel[가능성강화], probabilityImpact(점수), strength[verystrong/strong/medium/weak/veryweak], strengthLabel[매우높음/높음/보통/낮음/매우낮음], rationale, displayOrder)
            - `signpostMonitorings`: Signpost 모니터링 이력 (updatedAt, eventSummary, probability, probabilityDelta, rationale)
            """
    )
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

    @ApiOrder(7)
    @Operation(
        summary = "Scenario 상태 변경 API",
        description = """
            시나리오의 상태를 변경합니다. <br>
            대기(`pending`) -> 실현(`realized`) / 폐기(`discarded`). <br>

            <br>
            **`realized`(실현)로 변경 시, 같은 그룹의 나머지 시나리오는 모두 `discarded`(폐기)로 자동 변경됩니다.** (그룹 내 실현 시나리오는 항상 1개) <br>
            시나리오 그룹 status는 매 변경마다 재도출됩니다 — 실현 시나리오가 있으면 `realized`, 실현을 되돌리면 `created`로 환원. <br>
            (단, 실현 시 폐기로 캐스케이드된 나머지 시나리오는 되돌림 때 자동 복구되지 않습니다)<br>

            <br><br>
            ### 요청 본문
            - `status`: 변경할 판정 상태 (`pending` / `realized` / `discarded`)

            <br><br>
            ### 응답 필드
            - `scenarioId`, `status`, `statusLabel`(대기/실현/폐기)
            - `discardedScenarioIds`: 함께 '폐기'로 자동 변경된 시나리오 ID 목록 (`realized` 변경 시에만 채워지고, 그 외 빈 배열)
            """
    )
    @ApiResponse(responseCode = "200", description = "변경 성공")
    @PatchMapping("/{scenarioGroupId}/scenarios/{scenarioId}/status")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<ScenarioStatusRes> updateScenarioStatus(
        @Parameter(description = "시나리오 그룹 ID", example = "1") @PathVariable Long scenarioGroupId,
        @Parameter(description = "시나리오 ID", example = "11") @PathVariable Long scenarioId,
        @Valid @RequestBody ScenarioStatusUpdateReq request
    ) {
        return BaseResponseFactory.create(
            scenarioGroupService.updateScenarioStatus(scenarioGroupId, scenarioId, request.status())
        );
    }

    @ApiOrder(8)
    @Operation(
        summary = "Scenario Group 삭제 API",
        description = """
            시나리오 그룹을 삭제합니다. <br>
            그룹이 삭제되면 **그룹에 포함된 모든 시나리오도 함께 soft delete** 됩니다. <br>
            삭제된 그룹·시나리오는 목록/개별/상세/그래프/상태변경 조회에서 제외됩니다. <br>

            <br><br>
            ### 경로 변수
            - `scenarioGroupId`: 삭제할 시나리오 그룹 ID

            <br>
            존재하지 않거나 이미 삭제된 그룹이면 `404`를 반환합니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "삭제 성공")
    @DeleteMapping("/{scenarioGroupId}")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<Void> deleteScenarioGroup(
        @Parameter(description = "시나리오 그룹 ID", example = "1") @PathVariable Long scenarioGroupId
    ) {
        scenarioGroupService.deleteScenarioGroup(scenarioGroupId);
        return BaseResponseFactory.create(ResultCode.Success.OK);
    }

    @ApiOrder(9)
    @Operation(
        summary = "[mockapi 전용] 시나리오 상태/삭제 초기화 API",
        description = """
            상태 변경 API로 바꾼 **모든 시나리오 판정 상태**와 그룹 삭제 API로 삭제한 **모든 그룹/시나리오를 초기 설정값으로 되돌립니다.** <br>
            테스트/데모 반복 시 사용하는 mock 전용 API입니다. (실 DB 연동 없음 / 변경분은 mock 메모리에만 존재) <br>
            """
    )
    @ApiResponse(responseCode = "200", description = "초기화 성공")
    @PostMapping("/scenario-status/reset")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<Void> resetScenarioStatus() {
        scenarioGroupMockService.resetScenarioStatus(); // 상태/삭제/실행중 + materialize된 그룹 → fixture 복원
        scenarioDesignMockService.resetSessions();      // 설계 세션 초기화 → 이벤트 가용성까지 처음 상태로
        return BaseResponseFactory.create(ResultCode.Success.OK);
    }

}
