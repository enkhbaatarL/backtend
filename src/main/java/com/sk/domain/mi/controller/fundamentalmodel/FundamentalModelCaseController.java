package com.sk.domain.mi.controller.fundamentalmodel;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.enums.ResultCode;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.config.FundamentalModelMockProperties;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelCaseListReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelRunReq;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseCreateRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseListRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseStatusRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelRunAcceptRes;
import com.sk.domain.mi.service.fundamentalmodel.FundamentalModelCaseService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseSearchType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
        description = "MI Fundamental Model case 목록을 조회합니다. searchType과 keyword는 함께 입력해야 합니다."
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
        @Parameter(description = "Scenario Case 제목", example = "JKM price upside case") @RequestPart("title") String title,
        @Parameter(description = "Scenario Case 설명", example = "May demand shock scenario case")
        @RequestPart(value = "description", required = false) String description,
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
        summary = "MI Fundamental Model Scenario Case 실행 요청 API",
        description = "PENDING/COMPLETED/FAILED 상태의 case를 RUNNING 상태로 전이하고 runId를 발급합니다."
    )
    @ApiResponse(responseCode = "202", description = "비동기 실행 접수 성공")
    @PostMapping("/{caseId}/runs")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public ResponseEntity<BaseResponse<FundamentalModelRunAcceptRes>> runCase(
        @Parameter(description = "Scenario Case ID", example = "101") @PathVariable Long caseId,
        @Valid @RequestBody(required = false) FundamentalModelRunReq request
    ) {
        FundamentalModelRunAcceptRes response = fundamentalModelCaseService.runCase(caseId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(BaseResponseFactory.create(response));
    }

    @ApiOrder(4)
    @Operation(
        summary = "MI Fundamental Model Scenario Case 상태 조회 API",
        description = "case 기준 최신 실행 상태를 조회합니다. PENDING case는 latestRun이 null입니다."
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

    @ApiOrder(5)
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
