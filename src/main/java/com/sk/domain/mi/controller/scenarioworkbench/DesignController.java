package com.sk.domain.mi.controller.scenarioworkbench;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.enums.ResultCode;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.request.scenarioworkbench.DesignEventListReq;
import com.sk.domain.mi.dto.request.scenarioworkbench.ScenarioGenCallbackReq;
import com.sk.domain.mi.dto.request.scenarioworkbench.ScenarioGenReq;
import com.sk.domain.mi.dto.response.scenarioworkbench.DesignEventListRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.GeneratedScenarioRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.ScenarioGenAcceptRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.ScenarioGenStatusRes;
import com.sk.domain.mi.service.scenarioworkbench.DesignService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "5. Scenario Workbench Design API", description = "Scenario Workbench Design API입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/scenario-workbench/design")
@RequiredArgsConstructor
public class DesignController {

    private final DesignService designService;

    @ApiOrder(1)
    @Operation(
        summary = "Design 이벤트 목록 조회 API (with pagination)",
        description = """
            Design 탭의 이벤트 목록을 필터, 정렬, 페이징 조건으로 조회합니다. <br>
            Events 목록과 동일한 구조에 시나리오 생성 상태(scenarioGenStatus) 필드가 추가됩니다. <br>

            <br><br>
            ### 시나리오 생성 상태 (scenarioGenStatus)
            - `ready`: 시나리오 미생성 (생성 버튼 표시)
            - `processing`: 시나리오 생성 중 (생성중 표시)
            - `completed`: 시나리오 생성 완료 (완료 버튼 표시)
            """
    )
    @Parameter(name = "eventStatus", description = "이벤트 상태 (ALL / NEW / UPDATED). 기본값 ALL")
    @Parameter(name = "eventCategoryLv1", description = "이벤트 유형 Lv1. 복수 선택 가능")
    @Parameter(name = "period", description = "조회 기간. 기본값 SIX_MONTHS")
    @Parameter(name = "startDate", description = "커스텀 조회 시작일. period=CUSTOM 시 사용")
    @Parameter(name = "endDate", description = "커스텀 조회 종료일. period=CUSTOM 시 사용")
    @Parameter(name = "sort", description = "정렬 기준. 기본값 LATEST")
    @Parameter(name = "keyword", description = "검색 키워드")
    @Parameter(name = "hideCompleted", description = "완료 항목 숨기기. 기본값 false")
    @Parameter(name = "page", description = "페이지 번호. 기본값 1")
    @Parameter(name = "size", description = "페이지당 항목 수. 기본값 20")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/events")
    @RequiredPermission(
        roles = {Role.ADMIN, Role.USER},
        condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY
    )
    public BaseResponse<DesignEventListRes> getDesignEvents(@Valid @ParameterObject DesignEventListReq request) {
        return BaseResponseFactory.create(designService.getDesignEvents(request));
    }

    @ApiOrder(2)
    @Operation(
        summary = "시나리오 생성 요청 API",
        description = """
            이벤트를 기반으로 시나리오 생성을 요청합니다. <br>
            외부 API를 호출하여 비동기로 시나리오를 생성합니다. <br>

            <br><br>
            ### 요청 흐름
            1. `eventIds`로 대상 이벤트 지정 (Phase 1: 1건, Phase 2: N건)
            2. 서버에서 외부 API 호출 -> 요청 접수 (processing)
            3. 프론트는 `GET /generate/status` 폴링으로 상태 확인
            4. 외부 서비스 완료 시 콜백 -> completed
            """
    )
    @ApiResponse(responseCode = "200", description = "생성 요청 접수 성공")
    @PostMapping("/generate")
    @RequiredPermission(
        roles = {Role.ADMIN, Role.USER},
        condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY
    )
    public BaseResponse<ScenarioGenAcceptRes> generateScenarios(
        @Valid @RequestBody ScenarioGenReq request
    ) {
        ScenarioGenAcceptRes response = designService.generateScenarios(request);
        return BaseResponseFactory.create(response);
    }

    @ApiOrder(3)
    @Operation(
        summary = "시나리오 생성 콜백 수신 API",
        description = """
            외부 서비스가 시나리오 생성 완료/실패 시 호출하는 콜백 API입니다. <br>
            `requestId`로 요청을 매칭하여 상태를 갱신하고, 생성된 시나리오를 매핑합니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "콜백 처리 성공")
    @PostMapping("/callback")
    @RequiredPermission(roles = {Role.SYSTEM})
    public BaseResponse<Void> handleCallback(@Valid @RequestBody ScenarioGenCallbackReq request) {
        designService.handleCallback(request);
        return BaseResponseFactory.create(ResultCode.Success.OK);
    }

    @ApiOrder(4)
    @Operation(
        summary = "생성된 시나리오 조회 API",
        description = """
            생성 요청에 의해 생성된 시나리오 목록을 조회합니다. <br>
            Design 목록에서 `완료` 버튼 클릭 시 모달에 표시되는 데이터입니다. <br>
            요청 ID 기반 조회로, Phase 2에서 N개 이벤트 기반 요청도 대응 가능합니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/generate/{requestId}/scenarios")
    @RequiredPermission(
        roles = {Role.ADMIN, Role.USER},
        condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY
    )
    public BaseResponse<List<GeneratedScenarioRes>> getGeneratedScenarios(
        @Parameter(description = "생성 요청 ID", example = "1") @PathVariable Long requestId
    ) {
        return BaseResponseFactory.create(designService.getGeneratedScenarios(requestId));
    }

    @ApiOrder(6)
    @Operation(
        summary = "시나리오 생성 상태 폴링 API",
        description = """
            시나리오 생성 요청의 상태를 폴링 조회합니다. <br>
            프론트에서 목록의 processing 상태 항목들의 requestId를 전달하여 상태 갱신을 확인합니다. <br>

            <br><br>
            ### 사용 방법
            - 목록 조회 후 processing 상태인 항목의 requestId 목록을 수집
            - 주기적으로 본 API 호출하여 completed 전환 확인
            - 상태가 바뀌면 프론트에서 해당 행의 버튼 상태 갱신
            """
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/generate/status")
    @RequiredPermission(
        roles = {Role.ADMIN, Role.USER},
        condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY
    )
    public BaseResponse<List<ScenarioGenStatusRes>> getGenerateStatuses(
        @Parameter(description = "생성 요청 ID 목록", example = "1,2,3")
        @RequestParam List<Long> requestIds
    ) {
        return BaseResponseFactory.create(designService.getGenerateStatuses(requestIds));
    }

    @ApiOrder(5)
    @Operation(
        summary = "[mockapi 전용] 데이터 초기화 API",
        description = """
            Mock 데이터를 초기 상태(모든 이벤트 READY)로 리셋합니다. <br>
            테스트/데모 반복 시 사용합니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "초기화 성공")
    @PostMapping("/reset")
    @RequiredPermission(
        roles = {Role.ADMIN, Role.USER},
        condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY
    )
    public BaseResponse<Void> reset() {
        designService.reset();
        return BaseResponseFactory.create(ResultCode.Success.OK);
    }

}
