package com.sk.domain.mi.controller.fundamentalmodel;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.enums.ResultCode;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.config.FundamentalModelMockProperties;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelCaseListReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelCaseUpdateReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelTradeFlowsReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelVolumesReq;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseCreateRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseDetailRes;
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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Profile("!dedicated")
@Tag(name = "6. MI Fundamental Model Case API", description = "MI Fundamental Model Scenario Case mock API입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/fundamental-model/cases")
@RequiredArgsConstructor
public class FundamentalModelCaseController {

    private final FundamentalModelCaseService fundamentalModelCaseService;
    private final FundamentalModelMockProperties mockProperties;

    @ApiOrder(1)
    @Operation(
        summary = "MI Fundamental Model Scenario Case 목록 조회 API",
        description = "MI Fundamental Model case 목록을 조회합니다. keyword가 non-blank인 경우에만 searchType이 필수입니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
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

    @ApiOrder(2)
    @Operation(
        summary = "MI Fundamental Model Scenario Case 업로드 API",
        description = "파일 3개와 metadata를 업로드해 PENDING 상태의 Fundamental Model case를 생성합니다."
    )
    @ApiResponse(responseCode = "201", description = "생성 성공")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public ResponseEntity<BaseResponse<FundamentalModelCaseCreateRes>> createCase(
        @Parameter(description = "Scenario Case 제목", example = "JKM price upside case") @RequestParam("title") String title,
        @Parameter(description = "Scenario Case 설명", example = "May demand shock scenario case")
        @RequestParam(value = "description", required = false) String description,
        @Parameter(description = "모델 실행 입력 파일 3개") @RequestPart("files") List<MultipartFile> files
    ) {
        FundamentalModelCaseCreateRes response = fundamentalModelCaseService.createCase(
            title,
            description,
            files
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponseFactory.create(response));
    }

    @ApiOrder(3)
    @Operation(
        summary = "MI Fundamental Model Scenario Case 상세 조회 API",
        description = "case metadata, input/output 다운로드 파일 metadata, latestRun만 조회합니다. full run history와 result payload는 포함하지 않습니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
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
        description = "case title/description만 수정하고 최신 상세 응답을 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "수정 성공")
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
        description = "case를 physical delete하지 않고 soft delete 처리합니다."
    )
    @ApiResponse(responseCode = "200", description = "삭제 성공")
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
        description = "polling용 경량 상태를 조회합니다. PENDING case는 latestRun이 null이며 file metadata는 포함하지 않습니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
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
        summary = "MI Fundamental Model Volumes 필터 옵션 조회 API",
        description = "Volumes(Global/LNG) 및 Trade 탭의 필터 패널에 표시할 segment별 type/region/granularity 옵션 목록을 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/volumes/filter-options")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<FundamentalModelVolumesFilterOptionsRes> getVolumesFilterOptions() {
        return BaseResponseFactory.create(fundamentalModelCaseService.getVolumesFilterOptions());
    }

    @ApiOrder(9)
    @Operation(
        summary = "MI Fundamental Model Volumes 데이터 조회 API",
        description = "COMPLETED 상태의 case에 대해 segment/type/region/granularity 필터가 적용된 Volumes mock 데이터를 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
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
        @Parameter(description = "시간 단위. 미전달 시 MONTHLY입니다.", example = "MONTHLY")
        @RequestParam(required = false) FundamentalModelGranularity granularity
    ) {
        return BaseResponseFactory.create(fundamentalModelCaseService.getVolumes(
            caseId,
            new FundamentalModelVolumesReq(segment, types, regions, granularity)
        ));
    }

    @ApiOrder(10)
    @Operation(
        summary = "MI Fundamental Model Trade Flows 데이터 조회 API",
        description = "COMPLETED 상태의 case에 대해 selector/year 기준 Trade Flow sparse matrix 데이터를 조회합니다. fromCountries/toCountries 헤더 배열과 non-zero flows 배열로 응답합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
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
        @RequestParam(required = false) List<FundamentalModelRegion> regions
    ) {
        return BaseResponseFactory.create(fundamentalModelCaseService.getTradeFlows(
            caseId,
            new FundamentalModelTradeFlowsReq(selector, year, unit, types, regions)
        ));
    }

    @ApiOrder(11)
    @Operation(
        summary = "MI Fundamental Model Price 데이터 조회 API",
        description = "COMPLETED 상태의 case에 대해 Price chart, annualTable, monthlyTable mock 데이터를 조회합니다. "
            + "chart는 그래프 우상단 granularity 드롭다운(Monthly/Yearly)에 대응하는 monthlySeriesSets/yearlySeriesSets를 모두 포함합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{caseId}/price")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<FundamentalModelPriceRes> getPrice(
        @Parameter(description = "Scenario Case ID", example = "106") @PathVariable Long caseId
    ) {
        return BaseResponseFactory.create(fundamentalModelCaseService.getPrice(caseId));
    }

    @ApiOrder(12)
    @Operation(
        summary = "[mockapi 전용] MI Fundamental Model Scenario Case 데이터 초기화 API",
        description = "설정 flag가 활성화된 경우 mock case/run 데이터를 초기화합니다."
    )
    @ApiResponse(responseCode = "200", description = "초기화 성공")
    @PostMapping("/reset")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public ResponseEntity<BaseResponse<Void>> reset() {
        if (!mockProperties.getFundamentalModelCases().getApi().isReset()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(BaseResponseFactory.create(ResultCode.Error.RSC_NOT_FOUND));
        }

        fundamentalModelCaseService.reset();
        return ResponseEntity.ok(BaseResponseFactory.create(ResultCode.Success.OK));
    }
}
