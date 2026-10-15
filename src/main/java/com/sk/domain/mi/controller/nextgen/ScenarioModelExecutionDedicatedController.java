package com.sk.domain.mi.controller.nextgen;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.request.nextgen.ModelParamProposalExecutionReq;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionAcceptRes;
import com.sk.domain.mi.service.nextgen.ScenarioModelExecutionService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "05-1. Next Gen MI Model Execution API", description = "Next Gen MI MI 수급모델 실행 API입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/next-gen/scenarios/{scenarioId}")
@RequiredArgsConstructor
public class ScenarioModelExecutionDedicatedController {

    private final ScenarioModelExecutionService scenarioModelExecutionService;

    @ApiOrder(4)
    @Operation(
        summary = "MI 수급모델 실행 접수 API",
        description = "사용자가 업로드한 `DEMAND`, `SUPPLY`, `TRADE`, `OUTPUT` 입력 파일 4개로 MI 수급모델 실행 비동기 리소스 생성을 요청합니다. "
            + "네 역할 파일을 각각 정확히 1개씩 업로드해야 하며, `OUTPUT`은 실행 결과가 아니라 모델 실행 입력입니다."
    )
    @ApiResponse(responseCode = "202", description = "비동기 실행 접수 성공")
    @ApiResponse(
        responseCode = "400",
        description = "REQ_INVALID_PARAMETER: 파일이 정확히 4개가 아니거나 입력 역할이 누락·중복됨; FILE_INVALID: 빈 파일이 포함됨",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = BaseResponse.class)
        )
    )
    @PostMapping(value = "/model-executions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public ResponseEntity<BaseResponse<ModelExecutionAcceptRes>> submitModelExecution(
        @Parameter(description = "시나리오 ID", example = "1") @PathVariable Long scenarioId,
        @Parameter(description = "모델 실행 입력 파일 4개. 파일명에 DEMAND, SUPPLY, TRADE, OUTPUT 역할을 각각 정확히 1개 포함")
        @RequestPart("files") List<MultipartFile> files
    ) {
        ModelExecutionAcceptRes response = scenarioModelExecutionService.submitModelExecution(scenarioId, files);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(BaseResponseFactory.create(response));
    }

    @Deprecated
    @ApiOrder(5)
    @Operation(
        summary = "파라미터를 확정하고 인풋파일 생성하여 수급모델 실행 접수 API (deprecated)",
        deprecated = true,
        description = """
            deprecated - 시나리오 파라미터 비교 팝업 전환 후 그룹 일괄 실행 API(POST /scenario-groups/{scenarioGroupId}/model-executions)를 사용하세요.
            구 단건 팝업 호환을 위해 유지되며 FE 전환 완료 후 제거 예정입니다. 내부 파이프라인은 그룹 일괄 실행이 동일하게 재사용합니다.

            Agent 확정값을 DEMAND, SUPPLY, TRADE에 병합하고 OUTPUT 기준 원본을 더한 입력 Excel 4개로 MI 수급모델 실행을 비동기로 접수합니다.
            """
    )
    @ApiResponse(responseCode = "202", description = "비동기 실행 접수 성공")
    @PostMapping(value = "/model-executions/from-parameters", consumes = MediaType.APPLICATION_JSON_VALUE)
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public ResponseEntity<BaseResponse<ModelExecutionAcceptRes>> submitModelExecutionFromParameters(
        @Parameter(description = "시나리오 ID", example = "1") @PathVariable Long scenarioId,
        @Valid @RequestBody ModelParamProposalExecutionReq request
    ) {
        ModelExecutionAcceptRes response = scenarioModelExecutionService.submitModelExecution(scenarioId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(BaseResponseFactory.create(response));
    }
}
