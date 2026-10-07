package com.sk.domain.mi.controller.nextgen;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.request.nextgen.EventListReq;
import com.sk.domain.mi.dto.response.nextgen.EventDetailRes;
import com.sk.domain.mi.dto.response.nextgen.EventInstitutionOutlookRes;
import com.sk.domain.mi.dto.response.nextgen.EventLinkedScenarioGroupRes;
import com.sk.domain.mi.dto.response.nextgen.EventListRes;
import com.sk.domain.mi.dto.response.nextgen.EventRelatedRecordRes;
import com.sk.domain.mi.dto.response.nextgen.EventTypeL1Res;
import com.sk.domain.mi.dto.response.nextgen.design.EventDesignAvailabilityRes;
import com.sk.domain.mi.service.nextgen.EventService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Profile("!dedicated")
@Tag(name = "01. Next Gen MI Event API", description = "Next Gen MI Event API입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/next-gen/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @ApiOrder(1)
    @Operation(
        summary = "이벤트 유형 L1 목록 조회 API",
        description = """
            이벤트 유형 Level 1 마스터 목록을 조회합니다. <br>
            이벤트 목록 필터의 `eventTypeL1Id` 파라미터에 사용할 ID값을 제공합니다.

            <br><br>
            ### 응답 필드
            - `eventTypeL1Id`: 이벤트 유형 L1 PK (필터 파라미터에 사용)
            - `code`: 이벤트 유형 L1 코드
            - `label`: 이벤트 유형 L1 한글명
            """
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/event-types/lv1")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<List<EventTypeL1Res>> getEventTypesL1() {
        return BaseResponseFactory.create(eventService.getEventTypesL1());
    }

    @ApiOrder(2)
    @Operation(
        summary = "Event 목록 조회 API (with pagination)",
        description = """
            Event 목록을 필터, 정렬, 페이징 조건으로 조회합니다. <br>

            <br><br>
            ### 사용 가능한 파라미터
             - eventStatus: 이벤트 상태 / 기본값 all / example - eventStatus=new, eventStatus=updated
             - sort: 정렬 기준 / 기본값 latest(최신순) / 최신 연관 레코드 최초 생성일시(latestRecordFirstCreated) 기준 정렬 / example - sort=latest, sort=oldest
             - eventTypeL1Id: 이벤트 유형 Lv1 ID 필터 (복수 가능) / example - eventTypeL1Id=1&eventTypeL1Id=2
             - startDate: 커스텀 조회 시작일 / 최신 연관 레코드 최초 생성일시(latestRecordFirstCreated) 기준 / example - startDate=2025-09-01
             - endDate: 커스텀 조회 종료일 / 최신 연관 레코드 최초 생성일시(latestRecordFirstCreated) 기준 / example - endDate=2026-02-28
             - suggestedOnly: 시나리오화 추천 항목만 보기 / 기본값 false / example - suggestedOnly=true
             - keyword: 이벤트 제목 검색 키워드 / example - keyword=호르무즈
             - page: 기본값 1 / 1-based
             - size: 기본값 20

            <br><br>
            ### 이벤트 상태 (eventStatus)
            - `all`: 전체 이벤트
            - `new`: 신규 이벤트
            - `updated`: 갱신 이벤트

            <br><br>
            ### events.content 항목 필드
            - `eventId`: 이벤트 고유 식별자
            - `eventName`: 이벤트명
            - `eventTypeLv1` / `eventTypeLv1Label`: 이벤트 유형 Lv1 코드/라벨
            - `createdAt`: 생성일시
            - `updatedAt`: 수정일시
            - `latestRecordFirstCreated`: 최신 연관 레코드 최초 생성일시 (최신순 정렬 기준)
            - `status` / `statusLabel`: 신규(`new`) / 갱신(`updated`)
            - `sourceType` / `sourceTypeLabel`: 플랫폼 등록(`platform`) / 사용자 등록(`user`) — `user`면 사용자 등록 배지
            - `signalType` / `signalTypeLabel`: 신호(`signal`) / 소음(`noise`)
            - `isScenarioSuggested`: 시나리오화 추천 여부
            - `scenarioSuggestReason`: 시나리오화 추천 근거
            - `scenarioGroupCount`: 연결된 활성 시나리오 그룹 수 (생성중 포함, 실패 방치 그룹 제외) — 0이면 설계 아이콘, 1 이상이면 그룹 N 버튼
            - `indexImpacts`: 인덱스별 영향도 — 단기/중장기 중 영향 강도가 더 큰 시점 1개만 표기 (강도가 같으면 단기).
              - `indexName` / `marketImpactLevel` / `marketImpactLevelLabel` / `direction` / `directionLabel` / `impactTerm` / `impactTermLabel`
              - `impactTerm`: `short` 단기 / `mid_long` 중장기
              - `marketImpactLevel`: `high` / `medium` / `low` (영향없음이면 null)
              - `direction`: `bullish` 상승 / `bearish` 하락 / `negligible` 보합(영향없음)
            """
    )
    @Parameter(name = "eventStatus", description = "이벤트 상태 (`all` / `new` / `updated`, 대소문자 무관). 기본값 `all`")
    @Parameter(name = "eventTypeL1Id", description = "이벤트 유형 Lv1 ID. 복수 선택 가능")
    @Parameter(name = "startDate", description = "조회 시작일 (yyyy-MM-dd, 최신 연관 레코드 최초 생성일시 기준)")
    @Parameter(name = "endDate", description = "조회 종료일 (yyyy-MM-dd, 최신 연관 레코드 최초 생성일시 기준)")
    @Parameter(name = "sort", description = "정렬 기준 (`latest` / `oldest`, 대소문자 무관). 기본값 `latest`")
    @Parameter(name = "keyword", description = "이벤트 제목 검색 키워드")
    @Parameter(name = "suggestedOnly", description = "추천 항목만 보기. 기본값 false")
    @Parameter(name = "sourceType", description = "출처 (`platform` / `user`). 생략 시 전체")
    @Parameter(name = "mine", description = "내가 만든 이벤트만 보기. true면 출처는 `user`로 고정. 기본값 false")
    @Parameter(name = "page", description = "페이지 번호 (1-based). 기본값 1")
    @Parameter(name = "size", description = "페이지당 항목 수. 기본값 20")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<EventListRes> getEvents(@Valid @ParameterObject EventListReq request) {
        return BaseResponseFactory.create(eventService.getEvents(request));
    }

    @ApiOrder(3)
    @Operation(
        summary = "이벤트 연결 시나리오 그룹 목록 API (그룹 N 펼침 패널)",
        description = """
            이벤트에 연결된 시나리오 그룹 목록을 반환합니다 (이벤트 목록의 그룹 N 버튼 펼침 패널용). <br>
            멀티 이벤트 전환으로 한 이벤트가 여러 그룹에 속할 수 있으며, 그룹 최신순으로 정렬됩니다. <br>

            <br><br>
            ### 응답 필드 (그룹별)
            - `scenarioGroupId` / `title`: 그룹 식별자와 그룹명
            - `status` + `statusLabel`: 그룹 상태 (creating/created/realized)
            - `stage` + `stageLabel`, `designStep` + `designStepLabel`: 진행 단계 (상태 배지 1단계/2단계 표기용, 설계중일 때만)
            - `mine`: 설계중 그룹의 진행자가 로그인 사용자인지 — **false면 상태 배지에 잠금 아이콘을 함께 표시** (설계중이 아니면 null)
            - `events`: 그룹을 구성하는 이벤트명 목록 (표시 순서)

            <br><br>
            ### 참고
            - 설계중(creating)인데 최신 생성 작업이 실패로 끝난 그룹은 목록에서 제외됩니다 (잠정 규칙).
            - 이벤트 목록 응답의 `scenarioGroupCount`와 함께 사용합니다 — 0이면 설계 아이콘, 1 이상이면 그룹 N 버튼.
            """
    )
    @ApiResponse(responseCode = "200", description = "조회 성공 (연결된 그룹이 없으면 빈 목록)")
    @GetMapping("/{eventId}/scenario-groups")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<List<EventLinkedScenarioGroupRes>> getEventScenarioGroups(
        @Parameter(description = "이벤트 ID", example = "1523") @PathVariable Long eventId
    ) {
        return BaseResponseFactory.create(eventService.getEventScenarioGroups(eventId));
    }

    @ApiOrder(4)
    @Operation(
        summary = "Event 상세 조회 API",
        description = """
            Event 상세 페이지의 핵심 데이터를 조회합니다. <br>

            <br><br>
            ### 상위(top-level) 필드
            - `eventId`, `eventName`
            - `status` / `statusLabel`: 신규(`new`) / 갱신(`updated`)
            - `sourceType` / `sourceTypeLabel`: 플랫폼 등록(`platform`) / 사용자 등록(`user`) — `user`면 사용자 등록 배지
            - `registrantName`: 등록자명 (사용자 등록 이벤트만, 그 외 null)
            - `signalType` / `signalTypeLabel`: 신호(`signal`) / 소음(`noise`)
            - `isScenarioSuggested`: 시나리오화 추천 여부
            - `scenarioSuggestReason`: 시나리오화 추천 근거 (추천 아님일 때 null)
            - `eventTypeLv1` / `eventTypeLv1Label` / `eventTypeLv2` / `eventTypeLv2Label`
            - `createdAt`, `updatedAt`, `latestRecordFirstCreated`(최신 연관 레코드 최초 생성일시)
            - `institutionViewSynthesis`: 기관 전망 View 종합 — 기관전망 섹션 상단의 전체 요약

            <br><br>
            ### impactAnalysis 필드
            이벤트가 LNG 시장 주요 인덱스(JKM, TTF, Henry Hub)에 미치는 영향을 분석한 결과입니다.
            - `indexImpacts`: 인덱스별 영향도. 각 항목 `indexName`, `shortTerm`, `midLongTerm`
              - `shortTerm` / `midLongTerm`: `marketImpactLevel` / `marketImpactLevelLabel` / `direction` / `directionLabel`
              - `midLongTerm`: 중기·장기 영향도 평균(절대값 반올림·부호 유지). 둘 다 없으면 null
              - `marketImpactLevel`: `high`/`medium`/`low` (영향없음이면 null) · `direction`: `bullish`/`bearish`/`negligible`(보합)
            - `analysisText`: 전체 영향도 종합 요약 (`tmm_mi_impact_m.impact_summary`)
            - `eventTypeLv1` / `eventTypeLv2`: 이벤트 유형 분류 (Lv1: 8종, Lv2: 22종)
            - `sources`: 분석에 사용된 출처(provider) 목록

            <br><br>
            ### aiInsights 필드
            AI 에이전트가 생성한 이벤트 핵심 요약 및 상세 분석입니다.
            - `keySummaries`: 핵심 요약 포인트 목록
            - `detailedAnalysis`: 상세 분석 본문

            <br><br>
            ### driverTreeScoreboard 필드
            인덱스(JKM/TTF/HH)별로 driver 묶음을 분리해 표시합니다.
            - 각 항목: `indexName`, `drivers[]`
            - `drivers[]`: `lv1Id/lv1Label`, `lv2Id/lv2Label`, `lv3Id/lv3Label`, `shortTerm`, `midLongTerm`, `reason`

            <br><br>
            ### relatedEventScenarios 필드
            이벤트에 연결된 시나리오 그룹별 표 목록입니다 (그룹 오래된 순 스택). <br>
            **연결된 그룹이 없으면 빈 목록**이며, 이때 화면은 시나리오 설계 버튼을 노출합니다. 생성중(설계 미완료) 그룹은 제외됩니다.
            - `scenarioGroupId`, `groupName`
            - `dimensionColumns`: 동적 표 헤더 — 시나리오 그룹 목록/상세와 동일 구조
              - `key` / `label`: 분기항목 키/명 · `description` / `rationale`: 분기항목 설명/선정 근거 (헤더 ⓘ, v4 전용. v1은 null)
            - `scenarios[]`: `scenarioId`, `scenarioName`, `baseCase`,
              `scenarioRole` / `scenarioRoleLabel`(시나리오명 앞 역할 배지 — `ceiling` 상단 / `floor` 하단 / `mixed` 혼합, 역할 없으면 null),
              `probability`(원본 0~100),
              `adjustedProbability`(재분배 확률 — 폐기 제외 비례 재분배로 활성 합 100, 폐기=0, 폐기 없으면 probability와 동일),
              `status` / `statusLabel`(대기/실현/폐기)
            - `scenarios[].dimensions`: 시나리오가 선택한 분기값 셀 (dimensionColumns 순서 매칭)
              - `key` / `label`: 분기항목 키/명 · `value`: 선택된 분기값 표시 라벨 · `rationale`: 값 선정 근거 (셀 ⓘ, v4 전용. v1은 null)
            """
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{eventId}")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<EventDetailRes> getEventDetail(
        @Parameter(description = "이벤트 ID", example = "1") @PathVariable Long eventId
    ) {
        return BaseResponseFactory.create(eventService.getEventDetail(eventId));
    }

    @ApiOrder(5)
    @Operation(
        summary = "Event 기관 전망 조회 API (with pagination)",
        description = """
            Event 관련 기관별 시장 전망 정보를 페이징 조회합니다. <br>
            전망 일자(`viewDate`) 내림차순으로 내려갑니다. <br>

            <br><br>
            ### 사용 가능한 파라미터
             - eventId: 이벤트 ID (Path) / example - 1
             - page: 기본값 1 / 1-based
             - size: 기본값 10

            <br><br>
            ### content 항목 필드
            - `viewDate`: 전망 일자 (yyyy-MM-dd)
            - `institutionName`: 기관명
            - `outlookSummary`: 전망 요약
            """
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{eventId}/outlooks")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<PaginationRes<EventInstitutionOutlookRes>> getEventOutlooks(
        @Parameter(description = "이벤트 ID", example = "1") @PathVariable Long eventId,
        @Parameter(description = "페이지 번호", example = "1") @RequestParam(defaultValue = "1") @Min(1) int page,
        @Parameter(description = "페이지당 항목 수", example = "10") @RequestParam(defaultValue = "10") @Min(1) int size
    ) {
        return BaseResponseFactory.create(eventService.getEventOutlooks(eventId, page, size));
    }

    @ApiOrder(6)
    @Operation(
        summary = "Event Records 조회 API (with pagination)",
        description = """
            Event에 매핑된 Record(뉴스/리포트) 목록을 페이징 조회합니다. <br>
            발행 일시(`publishedAt`) 내림차순으로 내려갑니다. <br>

            <br><br>
            ### 사용 가능한 파라미터
             - eventId: 이벤트 ID (Path) / example - 1
             - page: 기본값 1 / 1-based
             - size: 기본값 10

            <br><br>
            ### content 항목 필드
            - `publishedAt`: Record 발행 일시
            - `recordType`: Record 종류 (NEWS, REPORT 등)
            - `title`: Record 제목
            - `summary`: Record 요약 (1~2줄)
            - `goodsNo`: 상품 번호 (gds_no)
            - `goodsDataNo`: 상품 데이터 번호 (gds_data_no). NEWS인 경우 null
            - `sourceObjId`: 원본 소스 ID (source_obj_id)
            - `isPermitted`: 현재 사용자(고객사)의 상품 조회 권한 여부 (goodsNo 기준)
            - `mappingReason`: 이 Record가 Event에 연결된 근거 (Event 후보 단계 산출물)
            - `mappingConfidence`: 매핑 신뢰도 (0.00~1.00)
            """
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{eventId}/records")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<PaginationRes<EventRelatedRecordRes>> getEventRecords(
        @Parameter(description = "이벤트 ID", example = "1") @PathVariable Long eventId,
        @Parameter(description = "페이지 번호", example = "1") @RequestParam(defaultValue = "1") @Min(1) int page,
        @Parameter(description = "페이지당 항목 수", example = "10") @RequestParam(defaultValue = "10") @Min(1) int size
    ) {
        return BaseResponseFactory.create(eventService.getEventRecords(eventId, page, size));
    }

}
