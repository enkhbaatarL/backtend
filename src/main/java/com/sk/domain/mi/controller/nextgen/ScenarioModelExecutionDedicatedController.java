package com.sk.domain.mi.controller.nextgen;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionAcceptRes;
import com.sk.domain.mi.service.nextgen.ScenarioModelExecutionService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
        description = "사용자가 업로드한 입력 파일 3개로 MI 수급모델 실행 비동기 리소스 생성을 요청합니다."
    )
    @ApiResponse(responseCode = "202", description = "비동기 실행 접수 성공")
    @PostMapping(value = "/model-executions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public ResponseEntity<BaseResponse<ModelExecutionAcceptRes>> submitModelExecution(
        @Parameter(description = "시나리오 ID", example = "1") @PathVariable Long scenarioId,
        @Parameter(description = "모델 실행 입력 파일 3개") @RequestPart("files") List<MultipartFile> files
    ) {
        ModelExecutionAcceptRes response = scenarioModelExecutionService.submitModelExecution(scenarioId, files);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(BaseResponseFactory.create(response));
    }
}
