package com.sk.domain.mi.controller.fundamentalmodel;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseCreateRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelRunAcceptRes;
import com.sk.domain.mi.service.fundamentalmodel.FundamentalModelCaseService;
import com.sk.domain.mi.service.fundamentalmodel.KubernetesJobService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "6. MI Fundamental Model Case API", description = "MI Fundamental Model Scenario Case API입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/fundamental-model/cases")
@RequiredArgsConstructor
public class FundamentalModelCaseDedicatedController {

    private final FundamentalModelCaseService fundamentalModelCaseService;
    private final KubernetesJobService kubernetesJobService;

    @ApiOrder(2)
    @Operation(
        summary = "MI Fundamental Model Scenario Case 업로드 API",
        description = "`DEMAND`, `SUPPLY`, `TRADE`, `OUTPUT` 입력 파일을 각각 정확히 1개씩 업로드해 "
            + "PENDING 상태의 Fundamental Model case를 생성합니다. `OUTPUT`은 실행 결과가 아니라 모델 실행 입력입니다. "
            + "파일 하나라도 업로드에 실패하면 먼저 업로드된 파일을 보상 삭제하고 Case 생성 요청 전체를 실패 처리합니다."
    )
    @ApiResponse(responseCode = "201", description = "생성 성공")
    @ApiResponse(
        responseCode = "400",
        description = "REQ_INVALID_PARAMETER: 파일이 정확히 4개가 아니거나 입력 역할이 누락·중복됨; FILE_INVALID: 빈 파일이 포함됨",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = BaseResponse.class)
        )
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public ResponseEntity<BaseResponse<FundamentalModelCaseCreateRes>> createCase(
        @Parameter(description = "Scenario Case 제목", example = "JKM price upside case") @RequestParam("title") String title,
        @Parameter(description = "Scenario Case 설명", example = "May demand shock scenario case")
        @RequestParam(value = "description", required = false) String description,
        @Parameter(description = "모델 실행 입력 파일 4개. 파일명에 DEMAND, SUPPLY, TRADE, OUTPUT 역할을 각각 정확히 1개 포함")
        @RequestPart("files") List<MultipartFile> files
    ) {
        FundamentalModelCaseCreateRes response = fundamentalModelCaseService.createCase(
            title,
            description,
            files
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponseFactory.create(response));
    }

    @ApiOrder(6)
    @Operation(
        summary = "MI Fundamental Model Scenario Case 실행 요청 API",
        description = "active `INDEPENDENT`의 PENDING 상태 case를 최초 실행하거나 FAILED 상태의 모델 호출을 재시도합니다. "
            + "대상 case를 RUNNING 상태로 전이하고 새 runId를 발급하며, COMPLETED 상태의 case는 재실행할 수 없습니다. "
            + "Case의 DEMAND, SUPPLY, TRADE, OUTPUT 입력 경로를 모델 Job에 전달하며, 하나라도 없으면 Job을 생성하지 않습니다. "
            + "`MI_BASE | SCENARIO`는 resource hiding 규칙에 따라 조회되지 않습니다."
    )
    @ApiResponse(responseCode = "202", description = "비동기 실행 접수 성공")
    @ApiResponse(
        responseCode = "400",
        description = "BIZ_RULE_VIOLATION: DEMAND, SUPPLY, TRADE, OUTPUT input storage path 중 하나 이상이 누락되어 Job을 생성할 수 없음",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = BaseResponse.class)
        )
    )
    @ApiResponse(
        responseCode = "404",
        description = "RSC_NOT_FOUND: active INDEPENDENT가 아니거나 소유하지 않은 case",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = BaseResponse.class)
        )
    )
    @ApiResponse(
        responseCode = "409",
        description = "BIZ_RULE_VIOLATION: case가 RUNNING 또는 COMPLETED 상태여서 실행할 수 없음",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = BaseResponse.class)
        )
    )
    @PostMapping("/{caseId}/runs")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public ResponseEntity<BaseResponse<FundamentalModelRunAcceptRes>> runCase(
        @Parameter(description = "Scenario Case ID", example = "101") @PathVariable Long caseId
    ) {
        FundamentalModelRunAcceptRes response = fundamentalModelCaseService.runCase(caseId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(BaseResponseFactory.create(response));
    }

    @ApiOrder(99)
    @Operation(
        summary = "[TEST] k8s Job 생성 권한 검증 API",
        description = "최소 k8s Job을 생성하여 백엔드의 Job 생성 권한(RBAC)을 검증합니다. 생성된 Job 이름을 반환하며, 권한이 없으면 에러가 발생합니다."
    )
    @ApiResponse(responseCode = "200", description = "Job 생성 성공 (생성된 Job 이름 반환)")
    @PostMapping("/test/k8s-job")
    public BaseResponse<String> testK8sJobCreation() {
        return BaseResponseFactory.create(kubernetesJobService.createPermissionTestJob());
    }

}
