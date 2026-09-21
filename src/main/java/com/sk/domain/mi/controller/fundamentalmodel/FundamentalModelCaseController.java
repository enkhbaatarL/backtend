package com.sk.domain.mi.controller.fundamentalmodel;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.enums.ResultCode;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelCaseListReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelCaseUpdateReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelTradeFlowsReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelVolumesReq;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseDetailOpenApiRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseDetailRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseListItemOpenApiRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseListItemRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseListOpenApiRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseListRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseStatusRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelPriceRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelTradeFlowsRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelVolumesFilterOptionsRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelVolumesRes;
import com.sk.domain.mi.service.fundamentalmodel.FundamentalModelCaseService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseSearchType;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelDataType;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelGranularity;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelRegion;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelSegment;
import com.sk.domain.shared.enums.fundamentalmodel.TradeFlowDirection;
import com.sk.domain.shared.enums.fundamentalmodel.TradeFlowSelector;
import com.sk.domain.shared.enums.fundamentalmodel.TradeFlowUnit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Profile("!dedicated")
@Tag(name = "6. MI Fundamental Model Case API", description = "MI Fundamental Model Scenario Case API입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/fundamental-model/cases")
@RequiredArgsConstructor
public class FundamentalModelCaseController {

    private final FundamentalModelCaseService fundamentalModelCaseService;

    @ApiOrder(1)
    @Operation(
        summary = "MI Fundamental Model 실행 Case 및 MI Base 목록 조회 API",
        description = """
            current MI Base와 그 외 실행 case를 각각 `miBase`, `cases`로 분리해 반환합니다.

            - `miBase`: 검색 조건과 무관하게 고정 반환되는 active `MI_BASE` 단건이며, 없으면 `null`입니다.
            - `cases`: 검색 조건이 적용된 active `INDEPENDENT` 목록이며, `createdAt DESC, caseId DESC`로 정렬됩니다.
            - `totalCount`: current MI Base 0~1건과 필터된 `cases.size()`의 합입니다.
            - `SCENARIO`는 두 영역에서 모두 제외됩니다.

            `keyword`가 non-blank인 경우에만 `searchType`이 필수입니다. 각 항목의 `execType`은 current 기준값과 일반 독립실행을 구분합니다.
            """
    )
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        useReturnTypeSchema = true,
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = FundamentalModelCaseListOpenApiRes.class),
            examples = @ExampleObject(
                name = "current MI Base와 독립실행 목록",
                value = """
                    {
                      "status": true,
                      "code": "SUC-000",
                      "message": "Success.",
                      "data": {
                        "totalCount": 2,
                        "miBase": {"caseId": 105, "title": "현재 기준", "status": "COMPLETED", "execType": "MI_BASE", "assigneeName": "홍길동", "createdAt": "2026-06-30T16:25:00", "updatedAt": "2026-06-30T16:25:00"},
                        "cases": [{"caseId": 110, "title": "독립 실행", "status": "COMPLETED", "execType": "INDEPENDENT", "assigneeName": "홍길동", "createdAt": "2026-06-30T17:30:00", "updatedAt": "2026-06-30T17:34:00"}]
                      }
                    }
                    """
            )
        )
    )
    @ApiResponse(
        responseCode = "400",
        description = "REQ_INVALID_PARAMETER: non-blank keyword에 searchType이 없음",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BaseResponse.class))
    )
    @GetMapping
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<FundamentalModelCaseListRes> listCases(
        @Parameter(description = "검색 대상 필드", example = "TITLE") @RequestParam(required = false) FundamentalModelCaseSearchType searchType,
        @Parameter(description = "검색 키워드", example = "JKM") @RequestParam(required = false) String keyword
    ) {
        return BaseResponseFactory.create(
            fundamentalModelCaseService.listCases(
                new FundamentalModelCaseListReq(searchType, keyword)
            )
        );
    }


    @ApiOrder(3)
    @Operation(
        summary = "MI Fundamental Model Scenario Case 상세 조회 API",
        description = "active `INDEPENDENT`와 current `MI_BASE`의 case metadata, input 파일명, output/intermediate/tradeOutput 다운로드 metadata, latestRun만 조회합니다. "
            + "input은 fileNm만 반환하고 output/intermediate/tradeOutput은 공통 File API 요청용 fileNo, fileNm, fileSize를 반환합니다. "
            + "네 파일 목록은 각각 fileNm을 대소문자 구분 없이 일반 문자열 오름차순으로 정렬하고, 동률이면 fileNo 오름차순으로 반환합니다. "
            + "output/intermediate/tradeOutput은 최신 실행의 같은 run_seq 기준이며, 최신 실행, run_seq 또는 파일이 없으면 빈 배열입니다. full run history와 result payload는 포함하지 않습니다. "
            + "`MI_BASE`는 결과 조회만 허용되며 상세 조회가 수정·삭제·재실행 권한을 넓히지 않습니다. `SCENARIO`와 soft-deleted case는 조회되지 않습니다."
    )
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = FundamentalModelCaseDetailOpenApiRes.class),
            examples = @ExampleObject(
                name = "current MI Base 상세",
                value = """
                    {
                      "status": true,
                      "code": "SUC-000",
                      "message": "Success.",
                      "data": {
                        "caseId": 105,
                        "title": "현재 기준",
                        "description": "현재 MI Base",
                        "status": "COMPLETED",
                        "execType": "MI_BASE",
                        "assigneeName": "홍길동",
                        "assigneeUserNo": 1,
                        "files": {
                          "input": [{"fileNm": "DEMAND.xlsm"}, {"fileNm": "SUPPLY.xlsm"}, {"fileNm": "TRADE.xlsm"}],
                          "output": [{"fileNo": 2011, "fileNm": "result.xlsx", "fileSize": 54321}],
                          "intermediate": [{"fileNo": 3011, "fileNm": "step1_demand_curve.csv", "fileSize": 1234}],
                          "tradeOutput": [{"fileNo": 4011, "fileNm": "trade_flow_result.xlsx", "fileSize": 67890}]
                        },
                        "latestRun": {
                          "runId": "20260630_162500_A1B2",
                          "status": "COMPLETED",
                          "requestedAt": "2026-06-30T16:20:00",
                          "startedAt": "2026-06-30T16:20:00",
                          "finishedAt": "2026-06-30T16:25:00",
                          "failureReason": null,
                          "errorMessage": null
                        },
                        "createdAt": "2026-06-30T16:20:00",
                        "updatedAt": "2026-06-30T16:25:00"
                      }
                    }
                    """
            )
        )
    )
    @ApiResponse(
        responseCode = "404",
        description = "RSC_NOT_FOUND: case 없음, soft delete 또는 SCENARIO",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BaseResponse.class))
    )
    @GetMapping("/{caseId}")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<FundamentalModelCaseDetailRes> getCaseDetail(
        @Parameter(description = "Scenario Case ID", example = "101") @PathVariable Long caseId
    ) {
        return BaseResponseFactory.create(fundamentalModelCaseService.getCaseDetail(caseId));
    }

    @ApiOrder(4)
    @Operation(
        summary = "MI Fundamental Model Scenario Case 수정 API",
        description = "active `INDEPENDENT` case의 title/description만 수정하고, files.tradeOutput을 포함한 파일 목록을 fileNm 대소문자 무시 오름차순으로 정렬한 최신 상세 응답을 반환합니다. `MI_BASE | SCENARIO`는 수정할 수 없습니다."
    )
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @ApiResponse(
        responseCode = "404",
        description = "RSC_NOT_FOUND: active INDEPENDENT가 아니거나 소유하지 않은 case",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BaseResponse.class))
    )
    @PatchMapping("/{caseId}")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<FundamentalModelCaseDetailRes> updateCase(
        @Parameter(description = "Scenario Case ID", example = "101") @PathVariable Long caseId,
        @Valid @RequestBody FundamentalModelCaseUpdateReq request
    ) {
        return BaseResponseFactory.create(fundamentalModelCaseService.updateCase(caseId, request));
    }

    @ApiOrder(5)
    @Operation(
        summary = "MI Fundamental Model Scenario Case 삭제 API",
        description = "active `INDEPENDENT` case를 physical delete하지 않고 soft delete 처리합니다. `MI_BASE | SCENARIO`는 삭제할 수 없습니다."
    )
    @ApiResponse(responseCode = "200", description = "삭제 성공")
    @ApiResponse(
        responseCode = "404",
        description = "RSC_NOT_FOUND: active INDEPENDENT가 아니거나 소유하지 않은 case",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BaseResponse.class))
    )
    @DeleteMapping("/{caseId}")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<Void> deleteCase(
        @Parameter(description = "Scenario Case ID", example = "101") @PathVariable Long caseId
    ) {
        fundamentalModelCaseService.deleteCase(caseId);
        return BaseResponseFactory.create(ResultCode.Success.OK);
    }

    @ApiOrder(7)
    @Operation(
        summary = "MI Fundamental Model Scenario Case 상태 조회 API",
        description = "active `INDEPENDENT | MI_BASE`의 polling용 경량 상태를 조회합니다. PENDING case는 latestRun이 null이며 file metadata는 포함하지 않습니다. `SCENARIO`와 soft-deleted case는 조회되지 않습니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
        responseCode = "404",
        description = "RSC_NOT_FOUND: case 없음, soft delete 또는 SCENARIO",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BaseResponse.class))
    )
    @GetMapping("/{caseId}/status")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<FundamentalModelCaseStatusRes> getCaseStatus(
        @Parameter(description = "Scenario Case ID", example = "101") @PathVariable Long caseId
    ) {
        return BaseResponseFactory.create(fundamentalModelCaseService.getCaseStatus(caseId));
    }

    @ApiOrder(8)
    @Operation(
        summary = "MI Fundamental Model 결과 필터 옵션 조회 API",
        description = """
            MI Fundamental Model의 Volumes/Trade 탭에서 사용할 필터 옵션을 조회합니다. <br>
            F.E는 응답 `segments[]`에서 현재 탭과 같은 `segment`를 찾아 Type, Region/Country, Granularity, 기간 필터를 구성합니다.

            <br><br>
            ### 호출 방법
            - Case 선택 전: 파라미터 없이 호출하여 정적 Type/Region/Granularity 옵션을 사용합니다.
            - GLOBAL/LNG 탭: `caseId`만 전달합니다. 해당 Case의 최신 완료 run에 실제로 존재하는 Region/Country와 YEARLY/MONTHLY 기간을 반환합니다.
            - TRADE 탭: `caseId`, `selector`, `year`를 모두 전달합니다. selector 또는 year가 바뀌면 변경된 조합으로 다시 호출합니다.

            <br><br>
            ### segment별 응답 필드
            - `types`: 해당 segment에서 선택할 수 있는 Type 옵션
            - `regions[].countries`: Region 아래에 속한 Country 옵션. `countries[].value`를 Volumes/Trade 조회 API의 `countryKeys`로 전달합니다.
            - GLOBAL/LNG의 `granularities`/`periods`: 선택 가능한 시간 단위와 YEARLY/MONTHLY 기간 옵션
            - TRADE는 단일 year 기준 matrix이므로 `granularities=[]`, `periods.yearly=[]`, `periods.monthly=[]`입니다.

            <br><br>
            ### 빈 값과 오류
            - `caseId` 미전달 시 모든 Region의 `countries=[]`이며, 기간 목록도 빈 배열입니다.
            - `caseId`만 전달하면 active `INDEPENDENT | MI_BASE`의 GLOBAL/LNG 실제 옵션, TRADE 정적 Region과 `countries=[]`를 반환합니다.
            - `selector`/`year`를 한 개만 전달하거나 `caseId` 없이 전달하면 400, Case가 COMPLETED 상태가 아니면 409를 반환합니다.
            - `MI_BASE` read 허용은 수정·삭제·재실행 권한을 넓히지 않습니다. `SCENARIO`와 soft-deleted case는 조회되지 않습니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
        responseCode = "400",
        description = "REQ_INVALID_PARAMETER: caseId/selector/year 조합 또는 범위 오류",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BaseResponse.class))
    )
    @ApiResponse(
        responseCode = "404",
        description = "RSC_NOT_FOUND: case 없음, FAILED, soft delete 또는 SCENARIO",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BaseResponse.class))
    )
    @ApiResponse(
        responseCode = "409",
        description = "BIZ_RULE_VIOLATION: case가 PENDING 또는 RUNNING",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BaseResponse.class))
    )
    @GetMapping("/volumes/filter-options")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<FundamentalModelVolumesFilterOptionsRes> getVolumesFilterOptions(
        @Parameter(description = "실제 Region/Country/기간 옵션을 조회할 Scenario Case ID. 미전달 시 정적 옵션만 반환합니다.", example = "106")
        @RequestParam(required = false) Long caseId,
        @Parameter(description = "TRADE 옵션 범위. year와 함께 전달해야 하며 caseId도 필요합니다.", example = "LNG")
        @RequestParam(required = false) TradeFlowSelector selector,
        @Parameter(description = "TRADE 옵션 연도. selector와 함께 전달해야 하며 2023~2030 범위입니다.", example = "2026")
        @RequestParam(required = false) Integer year
    ) {
        return BaseResponseFactory.create(fundamentalModelCaseService.getVolumesFilterOptions(caseId, selector, year));
    }

    @ApiOrder(9)
    @Operation(
        summary = "MI Fundamental Model Volumes 데이터 조회 API",
        description = "active `INDEPENDENT | MI_BASE` 중 COMPLETED 상태의 case에 대해 segment/type/region/granularity/periods 필터가 적용된 Volumes 데이터를 조회합니다. "
            + "periods는 선택한 기간만 columns와 row values에 포함하며, YEARLY는 yyyy, MONTHLY는 yyyy-MM 형식입니다. "
            + "`MI_BASE` read 허용은 mutation 권한을 넓히지 않으며 `SCENARIO`와 soft-deleted case는 조회되지 않습니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
        responseCode = "400",
        description = "REQ_INVALID_PARAMETER: 필수/enum/Country key/기간 형식 오류",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BaseResponse.class))
    )
    @ApiResponse(
        responseCode = "404",
        description = "RSC_NOT_FOUND: case 없음, FAILED, soft delete 또는 SCENARIO",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BaseResponse.class))
    )
    @ApiResponse(
        responseCode = "409",
        description = "BIZ_RULE_VIOLATION: case가 PENDING 또는 RUNNING",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BaseResponse.class))
    )
    @GetMapping("/{caseId}/volumes")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<FundamentalModelVolumesRes> getVolumes(
        @Parameter(description = "Scenario Case ID", example = "106") @PathVariable Long caseId,
        @Parameter(description = "조회 segment", required = true, example = "GLOBAL") @RequestParam FundamentalModelSegment segment,
        @Parameter(description = "데이터 타입 필터. 미전달 시 전체입니다.", example = "DEMAND,PRODUCTION")
        @RequestParam(required = false) List<FundamentalModelDataType> types,
        @Parameter(description = "지역 필터. 미전달 시 전체입니다.", example = "ASIA,EUROPE")
        @RequestParam(required = false) List<FundamentalModelRegion> regions,
        @Parameter(description = "Country key 필터. COUNTRY:ASIA:Japan 형식의 exact match이며 regions와 함께 전달하면 AND 조건입니다.", example = "COUNTRY:ASIA:Japan")
        @RequestParam(required = false) List<String> countryKeys,
        @Parameter(description = "시간 단위. 미전달 시 MONTHLY입니다.", example = "MONTHLY")
        @RequestParam(required = false) FundamentalModelGranularity granularity,
        @Parameter(description = "선택 기간. YEARLY는 yyyy, MONTHLY는 yyyy-MM 형식입니다. 미전달 시 전체 기간입니다.", example = "2026-01,2026-06")
        @RequestParam(required = false) List<String> periods
    ) {
        return BaseResponseFactory.create(fundamentalModelCaseService.getVolumes(
            caseId,
            new FundamentalModelVolumesReq(segment, types, regions, countryKeys, granularity, periods)
        ));
    }

    @ApiOrder(10)
    @Operation(
        summary = "MI Fundamental Model Trade Flows 데이터 조회 API",
        description = "active `INDEPENDENT | MI_BASE` 중 COMPLETED 상태의 case에 대해 selector/year 기준 Trade Flow sparse matrix 데이터를 조회합니다. "
            + "fromCountries/toCountries 헤더 배열과 non-zero flows 배열로 응답합니다. `MI_BASE` read 허용은 mutation 권한을 넓히지 않으며 "
            + "`SCENARIO`와 soft-deleted case는 조회되지 않습니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
        responseCode = "400",
        description = "REQ_INVALID_PARAMETER: selector/year/enum/Country key 오류",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BaseResponse.class))
    )
    @ApiResponse(
        responseCode = "404",
        description = "RSC_NOT_FOUND: case 없음, FAILED, soft delete 또는 SCENARIO",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BaseResponse.class))
    )
    @ApiResponse(
        responseCode = "409",
        description = "BIZ_RULE_VIOLATION: case가 PENDING 또는 RUNNING",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BaseResponse.class))
    )
    @GetMapping("/{caseId}/volumes/trade")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<FundamentalModelTradeFlowsRes> getTradeFlows(
        @Parameter(description = "Scenario Case ID", example = "106") @PathVariable Long caseId,
        @Parameter(description = "Trade 유형", required = true, example = "LNG") @RequestParam TradeFlowSelector selector,
        @Parameter(description = "조회 연도 (2023~2030)", required = true, example = "2026") @RequestParam Integer year,
        @Parameter(description = "수치 단위. 미전달 시 MTPA", example = "MTPA") @RequestParam(required = false) TradeFlowUnit unit,
        @Parameter(description = "필터 방향. 미전달 시 전체", example = "TO_COUNTRY,FROM_COUNTRY")
        @RequestParam(required = false) List<TradeFlowDirection> types,
        @Parameter(description = "지역 필터. 미전달 시 전체", example = "ASIA,EUROPE")
        @RequestParam(required = false) List<FundamentalModelRegion> regions,
        @Parameter(description = "Country key 필터. COUNTRY:ASIA:Japan 형식의 exact match이며 regions와 함께 전달하면 AND 조건입니다.", example = "COUNTRY:ASIA:Japan")
        @RequestParam(required = false) List<String> countryKeys
    ) {
        return BaseResponseFactory.create(fundamentalModelCaseService.getTradeFlows(
            caseId,
            new FundamentalModelTradeFlowsReq(selector, year, unit, types, regions, countryKeys)
        ));
    }

    @ApiOrder(11)
    @Operation(
        summary = "MI Fundamental Model Price 데이터 조회 API",
        description = "active `INDEPENDENT | MI_BASE` 중 COMPLETED 상태의 case에 대해 Price chart, annualTable, monthlyTable 데이터를 조회합니다. "
            + "chart는 그래프 우상단 granularity 드롭다운(Monthly/Yearly)에 대응하는 monthlySeriesSets/yearlySeriesSets를 모두 포함합니다. "
            + "`MI_BASE` read 허용은 mutation 권한을 넓히지 않으며 `SCENARIO`와 soft-deleted case는 조회되지 않습니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
        responseCode = "404",
        description = "RSC_NOT_FOUND: case 없음, FAILED, soft delete 또는 SCENARIO",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BaseResponse.class))
    )
    @ApiResponse(
        responseCode = "409",
        description = "BIZ_RULE_VIOLATION: case가 PENDING 또는 RUNNING",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BaseResponse.class))
    )
    @GetMapping("/{caseId}/price")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<FundamentalModelPriceRes> getPrice(
        @Parameter(description = "Scenario Case ID", example = "2") @PathVariable Long caseId
    ) {
        return BaseResponseFactory.create(fundamentalModelCaseService.getPrice(caseId));
    }

    @ApiOrder(12)
    @Operation(
        summary = "독립실행 case MI Base 지정 API",
        description = """
            완료된 active `INDEPENDENT` case를 전역 current MI Base로 지정합니다. 기존 current MI Base는 같은 transaction에서 `INDEPENDENT`로 복귀합니다.

            같은 current case를 다시 지정하면 latest completed run, input 3종, Price 결과 적격성을 다시 검증한 뒤 멱등하게 변경 없이 `200 OK`를 반환합니다. 지정 성공은 이후 새로 시작하는 수급모델 input 생성부터 적용되며 기존 실행에는 소급 적용되지 않습니다. 성공 또는 지정 경쟁 충돌 뒤에는 `GET /cases`로 current를 다시 조회합니다.

            이 기능은 DBA가 `actv_base_yn` generated column과 `tmd_mdl_scnrcasem_uq02` unique constraint를 먼저 적용한 환경에서만 활성화해야 합니다.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "지정 성공 또는 같은 current MI Base의 멱등 재지정",
            useReturnTypeSchema = true,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = FundamentalModelCaseListItemOpenApiRes.class),
                examples = @ExampleObject(
                    name = "MI Base 지정 성공",
                    value = """
                        {
                          "status": true,
                          "code": "SUC-000",
                          "message": "Success.",
                          "data": {"caseId": 110, "title": "독립 실행", "status": "COMPLETED", "execType": "MI_BASE", "assigneeName": "홍길동", "createdAt": "2026-06-30T17:30:00", "updatedAt": "2026-07-23T16:50:00"}
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "REQ_INVALID_PARAMETER: caseId가 1 미만이거나 형식이 잘못됨",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BaseResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "인증되지 않은 요청",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BaseResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "허용되지 않은 role 또는 permission condition",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BaseResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "RSC_NOT_FOUND: case 없음, soft delete 또는 SCENARIO/지정 불가 MI_BASE",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BaseResponse.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "BIZ_RULE_VIOLATION: 상태·run/input/Price 부적격 또는 지정 경쟁 충돌",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BaseResponse.class))
        )
    })
    @PutMapping("/{caseId}/mi-base")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<FundamentalModelCaseListItemRes> designateMiBase(
        @Parameter(description = "MI Base로 지정할 완료 독립실행 Scenario Case ID", example = "110")
        @PathVariable @Min(1) Long caseId
    ) {
        return BaseResponseFactory.create(fundamentalModelCaseService.designateMiBase(caseId));
    }
}
