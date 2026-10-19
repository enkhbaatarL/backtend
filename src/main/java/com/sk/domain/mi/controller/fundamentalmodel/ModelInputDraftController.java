package com.sk.domain.mi.controller.fundamentalmodel;

import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.exception.CustomAuthorizationException;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.response.fundamentalmodel.ModelInputDraftResponses.*;
import com.sk.domain.mi.service.fundamentalmodel.input.InputDraftException;
import com.sk.domain.mi.service.fundamentalmodel.input.InputDraftJson;
import com.sk.domain.mi.service.fundamentalmodel.input.ModelInputDraftService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Profile("!dedicated")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/mi/fundamental-model", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "6. MI Fundamental Model Input Draft API", description = "개인 Draft 생성·조회와 변경 요청의 공통 접수·결과 조회")
public class ModelInputDraftController {
    private final ModelInputDraftService service;
    private final InputDraftJson json;

    public record CreateRequest(
        @Schema(description = "원본 구조 조회에서 받은 비교 기준", requiredMode = Schema.RequiredMode.REQUIRED,
            pattern = "^sv1_[0-9a-f]{64}$", example = "sv1_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
        String sourceVersion) {}

    @GetMapping("/cases/{caseId}/inputs/structure")
    @Operation(summary = "원본 입력 구조 조회", description = "active INDEPENDENT 또는 MI_BASE Case의 네 파일 구조와 sourceVersion을 조회합니다. 표 미선택 상태이므로 tableData는 null입니다. 회사 문맥은 인증 정보로 결정합니다.")
    @ApiResponse(responseCode = "200", description = "DEMAND·SUPPLY·TRADE·OUTPUT 순서의 원본 구조", content = @Content(schema = @Schema(implementation = SourceEnvelope.class)))
    @ApiResponse(responseCode = "400", description = "REQ_INVALID_PARAMETER: 잘못된 Case ID", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = NullErrorEnvelope.class)))
    @ApiResponse(responseCode = "404", description = "RSC_NOT_FOUND: 사용 가능한 원본 Case가 없음", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = NullErrorEnvelope.class)))
    @ApiResponse(responseCode = "409", description = "BIZ_RULE_VIOLATION: 원본 변경 또는 조회 정의 미완료", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = NullErrorEnvelope.class)))
    @ApiResponse(responseCode = "500", description = "SYS_INTERNAL_ERROR: 저장값 판독 또는 내부 처리 실패. data는 null", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = NullErrorEnvelope.class)))
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public ResponseEntity<byte[]> source(
        @Parameter(description = "원본 Case ID", schema = @Schema(type = "integer", format = "int64", minimum = "1"), example = "103") @PathVariable String caseId) {
        return success(service.sourceStructure(positive(caseId)));
    }

    @PostMapping(value = "/cases/{caseId}/input-drafts", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "개인 Draft 생성 또는 재개", description = "같은 회사·사용자·원본 Case의 편집 중 Draft가 있으면 재개합니다. 최초 생성 시 원본 네 파일과 행 ID를 하나의 DB 저장으로 고정합니다. 생성 여부는 created로 구분하며 두 경우 모두 200입니다. 표 값 수정과 최종저장은 이 API에 포함되지 않습니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(schema = @Schema(implementation = CreateRequest.class)))
    @ApiResponse(responseCode = "200", description = "신규 생성 또는 기존 Draft 재개", content = @Content(schema = @Schema(implementation = CreatedEnvelope.class), examples = @ExampleObject(value = """
        {"status":true,"code":"SUC-000","message":"Success.","data":{"draftId":"aefb60dc-18b8-4ac2-beb4-a4d499950765","sourceCaseId":103,"sourceKey":"sc1_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","sourceVersion":"sv1_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","status":"DRAFT","statusLabel":"편집 중","revision":0,"activeOperationId":null,"activeOperationEndpoint":null,"finalizedCaseId":null,"draftEndpoint":"/api/v1/mi/fundamental-model/input-drafts/aefb60dc-18b8-4ac2-beb4-a4d499950765","created":true}}
        """)))
    @ApiResponse(responseCode = "400", description = "REQ_INVALID_PARAMETER 또는 REQ_INVALID_JSON: 잘못된 요청", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemEnvelope.class)))
    @ApiResponse(responseCode = "404", description = "RSC_NOT_FOUND: 사용 가능한 원본 Case가 없음", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemEnvelope.class)))
    @ApiResponse(responseCode = "409", description = "BIZ_RULE_VIOLATION: SOURCE_CHANGED, DRAFT_SOURCE_CONFLICT, POLICY_INCOMPLETE 또는 POLICY_UNSUPPORTED", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemEnvelope.class)))
    @ApiResponse(responseCode = "413", description = "REQ_INVALID_PARAMETER: UTF-8 본문이 1MiB를 초과함", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemEnvelope.class)))
    @ApiResponse(responseCode = "500", description = "SYS_INTERNAL_ERROR: 저장값 판독 또는 내부 처리 실패. data는 null", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemEnvelope.class)))
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public ResponseEntity<byte[]> create(
        @Parameter(description = "원본 Case ID", schema = @Schema(type = "integer", format = "int64", minimum = "1"), example = "103") @PathVariable String caseId,
        HttpServletRequest request) throws IOException {
        return success(service.create(positive(caseId), json.sourceVersion(request.getInputStream())));
    }

    @GetMapping("/input-drafts/{draftId}")
    @Operation(summary = "개인 Draft 구조 재조회", description = "현재 회사·사용자가 소유한 Draft의 저장된 성공 revision과 구조를 반환합니다. 진행 작업 중에도 직전 성공 상태를 조회합니다. 다른 소유자의 Draft는 존재 여부를 공개하지 않고 404입니다. tableData는 null이며 저장 이력 해석 실패는 500입니다.")
    @ApiResponse(responseCode = "200", description = "Draft 구조", content = @Content(schema = @Schema(implementation = StructureEnvelope.class)))
    @ApiResponse(responseCode = "400", description = "REQ_INVALID_PARAMETER: 잘못된 Draft ID", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemEnvelope.class)))
    @ApiResponse(responseCode = "404", description = "RSC_NOT_FOUND: 본인 Draft가 없음", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemEnvelope.class)))
    @ApiResponse(responseCode = "409", description = "BIZ_RULE_VIOLATION: 원본 변경 또는 지원하지 않는 저장 상태", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemEnvelope.class)))
    @ApiResponse(responseCode = "500", description = "SYS_INTERNAL_ERROR: 저장값 판독 또는 내부 처리 실패. data는 null", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemEnvelope.class)))
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public ResponseEntity<byte[]> structure(
        @Parameter(description = "소문자 UUID Draft ID", schema = @Schema(format = "uuid")) @PathVariable String draftId) {
        return success(service.structure(uuid(draftId)));
    }

    @GetMapping("/input-drafts/{draftId}/table-data")
    @Operation(summary = "Draft 선택 표 조회", description = "구조 조회의 revision과 tableId를 보내 저장된 성공 값, 안정 행 ID와 현재 위치·편집 metadata를 조회합니다. 원본과 성공 이력을 복원하며 수식을 재계산하지 않습니다. 본문 최대 2,000행과 전체 응답 2MiB 범위에서 행을 나누며 다음 조회는 page.nextStartRow를 사용합니다. 헤더는 매번 포함됩니다. 실제 처리기와 worker 설정이 준비된 F-O-01 지정 표의 SET만 READY로 제공하며 다른 동작은 기존 미완비/미지원 상태를 유지합니다.")
    @ApiResponse(responseCode = "200", description = "선택 표와 다음 페이지 위치", content = @Content(schema = @Schema(implementation = TableEnvelope.class)))
    @ApiResponse(responseCode = "400", description = "REQ_INVALID_PARAMETER: 잘못된 ID·revision·시작행", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemEnvelope.class)))
    @ApiResponse(responseCode = "404", description = "RSC_NOT_FOUND: 본인 Draft 또는 선택 표가 없음", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemEnvelope.class)))
    @ApiResponse(responseCode = "409", description = "BIZ_RULE_VIOLATION: REVISION_CONFLICT, SOURCE_CHANGED 또는 정책 미완료", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemEnvelope.class)))
    @ApiResponse(responseCode = "422", description = "BIZ_RULE_VIOLATION: RESPONSE_TOO_LARGE, metadata와 한 행만으로 응답 한도를 초과함", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemEnvelope.class)))
    @ApiResponse(responseCode = "500", description = "SYS_INTERNAL_ERROR: 저장값 판독 또는 내부 처리 실패. data는 null", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemEnvelope.class)))
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public ResponseEntity<byte[]> table(
        @Parameter(description = "소문자 UUID Draft ID", schema = @Schema(format = "uuid")) @PathVariable String draftId,
        @Parameter(description = "구조 조회의 성공 저장 번호", required = true, schema = @Schema(type = "integer", format = "int64", minimum = "0", maximum = "9007199254740991")) @RequestParam(required = false) String revision,
        @Parameter(description = "구조 응답의 표 ID", required = true) @RequestParam(required = false) String tableId,
        @Parameter(description = "본문 시작 전역 행. 생략 시 첫 본문 행", schema = @Schema(type = "integer", minimum = "1", maximum = "1048576")) @RequestParam(required = false) String startRow) {
        if (tableId == null || tableId.isBlank() || tableId.length() > 1000) throw InputDraftException.invalid();
        long rev = nonnegative(revision);
        if (rev > 9007199254740991L) throw InputDraftException.invalid();
        Long first = startRow == null ? null : positive(startRow);
        if (first != null && first > 1048576) throw InputDraftException.invalid();
        return success(service.table(uuid(draftId), rev, tableId, first == null ? null : first.intValue()));
    }

    @Schema(requiredProperties={"requestId","baseRevision","operations"},additionalProperties=Schema.AdditionalPropertiesValue.FALSE)
    public record ChangeRequestSchema(
        @Schema(description="새 저장마다 생성하는 소문자 UUID. 응답 유실은 동일 body와 함께 재전송",format="uuid",
            pattern="^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$") String requestId,
        @Schema(description="EDT-02/03의 성공 저장 번호",minimum="0",maximum="9007199254740991",type="integer",format="int64") long baseRevision,
        @io.swagger.v3.oas.annotations.media.ArraySchema(minItems=1,maxItems=500,
            schema=@Schema(oneOf={AddRowCommand.class,SetCommand.class,ClearCommand.class,DeleteRowCommand.class}))
        List<Object> operations) {}
    @Schema(requiredProperties={"type","commandId","tableId","clientRowKey","values"},additionalProperties=Schema.AdditionalPropertiesValue.FALSE)
    public record AddRowCommand(@Schema(allowableValues={"ADD_ROW"}) String type,
        @Schema(pattern="^[A-Za-z][A-Za-z0-9_-]{0,63}$") String commandId,
        @Schema(description="구조 응답의 표 ID") String tableId,
        @Schema(pattern="^[A-Za-z][A-Za-z0-9_-]{0,63}$",description="이 요청 안에서 유일한 임시키") String clientRowKey,
        @Schema(description="안정 fieldKey별 TEXT/INTEGER/DECIMAL 값. null/boolean/객체/배열은 금지") java.util.Map<String,Object> values) {}
    @Schema(requiredProperties={"type","commandId","tableId","rowRef","fieldKey","value"},additionalProperties=Schema.AdditionalPropertiesValue.FALSE)
    public record SetCommand(@Schema(allowableValues={"SET"}) String type,
        @Schema(pattern="^[A-Za-z][A-Za-z0-9_-]{0,63}$") String commandId,String tableId,RowReference rowRef,
        @Schema(pattern="^[A-Za-z][A-Za-z0-9_-]{0,63}$") String fieldKey,
        @Schema(description="TEXT 최대4096자 또는 정확한 JSON number. null/boolean/객체/배열은 금지") Object value) {}
    @Schema(requiredProperties={"type","commandId","tableId","rowRef","fieldKey"},additionalProperties=Schema.AdditionalPropertiesValue.FALSE)
    public record ClearCommand(@Schema(allowableValues={"CLEAR"}) String type,
        @Schema(pattern="^[A-Za-z][A-Za-z0-9_-]{0,63}$") String commandId,String tableId,RowReference rowRef,
        @Schema(pattern="^[A-Za-z][A-Za-z0-9_-]{0,63}$") String fieldKey) {}
    @Schema(requiredProperties={"type","commandId","tableId","rowRef"},additionalProperties=Schema.AdditionalPropertiesValue.FALSE)
    public record DeleteRowCommand(@Schema(allowableValues={"DELETE_ROW"}) String type,
        @Schema(pattern="^[A-Za-z][A-Za-z0-9_-]{0,63}$") String commandId,String tableId,RowReference rowRef) {}
    @Schema(description="rowId 또는 앞선 ADD_ROW의 clientRowKey 중 정확히 하나",minProperties=1,maxProperties=1,additionalProperties=Schema.AdditionalPropertiesValue.FALSE)
    public record RowReference(@Schema(format="uuid",description="기존 논리 행의 소문자 UUID") String rowId,
        @Schema(pattern="^[A-Za-z][A-Za-z0-9_-]{0,63}$",description="같은 표의 앞선 ADD_ROW 임시키") String clientRowKey) {}

    @PostMapping(value="/input-drafts/{draftId}/change-requests",consumes=MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary="Draft 변경 임시저장 접수",description="EDT-02/03의 성공 revision과 순서 있는 명령을 제출합니다. 같은 requestId의 같은 의미 요청은 기존 작업을 반환하며 숫자3.20과3.2는 같습니다. 진행 중202, 종료 후 재전송200입니다. 202는 저장 성공이 아니며 statusEndpoint로 결과를 조회합니다. 현재는 별도 발행의 F-O-01(USCostofSupply B8 한 슬롯, breakevenPrice 숫자0.35 SET)만 지원합니다. 승인 사례는 추가 계산 없이 다른 원본 셀·수식·저장값·구조를 보존합니다. 다른 동작은 정책 미완비/미지원 오류로 거부됩니다. 본문은 UTF-8 1MiB, 깊이8, ADD_ROW 최대100개입니다. 회사·사용자는 인증 문맥에서 결정합니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required=true,content=@Content(schema=@Schema(implementation=ChangeRequestSchema.class)))
    @ApiResponse(responseCode="202",description="SUC-000: 신규 또는 진행 중 동일 요청 접수. 실제 임시저장 성공 아님",content=@Content(schema=@Schema(implementation=OperationEnvelope.class)))
    @ApiResponse(responseCode="200",description="SUC-000: 종료된 동일 요청의 기존 결과",content=@Content(schema=@Schema(implementation=OperationEnvelope.class)))
    @ApiResponse(responseCode="400",description="REQ-003/REQ-005: 잘못된 명령/JSON·중복 key/임시키·타입",content=@Content(schema=@Schema(implementation=ProblemEnvelope.class)))
    @ApiResponse(responseCode="404",description="RSC-001: 본인 Draft 또는 접근 가능한 원본/표 없음. data:null",content=@Content(schema=@Schema(implementation=ProblemEnvelope.class)))
    @ApiResponse(responseCode="409",description="BIZ-001: DRAFT_BUSY(REVISION_CONFLICT보다 우선), IDEMPOTENCY_CONFLICT, SOURCE_CHANGED, DRAFT_NOT_EDITABLE, POLICY_INCOMPLETE/POLICY_UNSUPPORTED",content=@Content(schema=@Schema(implementation=ProblemEnvelope.class)))
    @ApiResponse(responseCode="413",description="REQ-003: PAYLOAD_TOO_LARGE",content=@Content(schema=@Schema(implementation=ProblemEnvelope.class)))
    @ApiResponse(responseCode="422",description="BIZ-001: VALIDATION_FAILED, 허용되지 않은 동작",content=@Content(schema=@Schema(implementation=ProblemEnvelope.class)))
    @ApiResponse(responseCode="500",description="SYS-001: 내부 저장/판독 실패. data:null. 접수 여부 불명은 같은 requestId 재전송",content=@Content(schema=@Schema(implementation=ProblemEnvelope.class)))
    @RequiredPermissionRules(rules={
        @PermissionRule(role=Role.USER,conditions={PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role=Role.ADMIN,conditions={PermissionCondition.THIRD_PARTY_ADMIN_ONLY})})
    public ResponseEntity<byte[]> change(@Parameter(description="본인 Draft의 소문자 UUID",schema=@Schema(format="uuid")) @PathVariable String draftId,
        HttpServletRequest request)throws IOException {
        var result=service.submitChange(uuid(draftId),json.changeRequest(request.getInputStream()));
        return ResponseEntity.status(result.status().equals("QUEUED")||result.status().equals("RUNNING")?202:200)
            .contentType(MediaType.APPLICATION_JSON).body(json.bytes(BaseResponseFactory.create(result)));
    }

    @GetMapping("/input-drafts/{draftId}/change-requests/{operationId}")
    @Operation(summary="Draft 임시저장 처리 결과 조회",description="접수 응답 유실 또는 대기 중인 작업을 같은 회사·사용자 문맥으로 조회합니다. QUEUED/RUNNING은2초 간격으로 조회하고 SUCCEEDED/FAILED면 종료합니다. 실패 작업 조회도 HTTP200/SUC-000이며 data.failure에 사유가 있습니다. SUCCEEDED일 때만 resultRevision으로 Draft를 다시 조회합니다. source 기준 변경 뒤에도 기존 기록을 읽을 수 있고 원본 접근권한 상실은404입니다. 기한 경과는 DB의 실제 상태 확인 뒤 TIMEOUT으로 확정합니다.")
    @ApiResponse(responseCode="200",description="SUC-000: 기록된 작업 결과. FAILED도 조회 성공",content=@Content(schema=@Schema(implementation=OperationEnvelope.class),examples=@ExampleObject(value="""
        {"status":true,"code":"SUC-000","message":"Success.","data":{"operationId":"aefb60dc-18b8-4ac2-beb4-a4d499950766","requestId":"aefb60dc-18b8-4ac2-beb4-a4d499950767","draftId":"aefb60dc-18b8-4ac2-beb4-a4d499950765","status":"FAILED","statusLabel":"저장 실패","baseRevision":0,"resultRevision":null,"rowMappings":[],"affectedTableIds":[],"failure":{"reason":"TIMEOUT","currentRevision":0,"issues":[]},"statusEndpoint":"/api/v1/mi/fundamental-model/input-drafts/aefb60dc-18b8-4ac2-beb4-a4d499950765/change-requests/aefb60dc-18b8-4ac2-beb4-a4d499950766"}}
        """)))
    @ApiResponse(responseCode="400",description="REQ-003: 잘못된 UUID",content=@Content(schema=@Schema(implementation=ProblemEnvelope.class)))
    @ApiResponse(responseCode="404",description="RSC-001: 본인 Draft/CHANGE 작업 또는 접근 가능한 원본 없음. data:null",content=@Content(schema=@Schema(implementation=ProblemEnvelope.class)))
    @ApiResponse(responseCode="500",description="SYS-001: 내부 저장/판독 실패. data:null",content=@Content(schema=@Schema(implementation=ProblemEnvelope.class)))
    @RequiredPermissionRules(rules={
        @PermissionRule(role=Role.USER,conditions={PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role=Role.ADMIN,conditions={PermissionCondition.THIRD_PARTY_ADMIN_ONLY})})
    public ResponseEntity<byte[]> changeOperation(@Parameter(description="본인 Draft의 소문자 UUID",schema=@Schema(format="uuid")) @PathVariable String draftId,
        @Parameter(description="접수 응답의 소문자 작업 UUID",schema=@Schema(format="uuid")) @PathVariable String operationId) {
        return success(service.changeOperation(uuid(draftId),uuid(operationId)));
    }

    @Schema(requiredProperties={"requestId","baseRevision","title"},additionalProperties=Schema.AdditionalPropertiesValue.FALSE)
    public record FinalizationRequestSchema(
        @Schema(description="CHANGE와 공통 중복 키. 재전송은 같은 UUID와 내용",format="uuid",pattern="^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$") String requestId,
        @Schema(description="최종저장할 현재 성공 revision. 0도 허용",type="integer",format="int64",minimum="0",maximum="9007199254740991") long baseRevision,
        @Schema(description="새 Case 제목. Java trim 후 비어 있지 않고200자 이하",minLength=1,maxLength=200) String title,
        @Schema(description="새 Case 설명. 생략=null, 빈 문자열은 보존, UTF-8 65,535bytes 이하",nullable=true,types={"string","null"}) String description) {}

    @PostMapping(value="/input-drafts/{draftId}/finalizations",consumes=MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary="Draft 최종저장 접수",description="미저장 변경을 먼저 임시저장한 뒤 현재 성공 revision과 새 Case 제목을 제출합니다. revision0도 허용합니다. 고정 원본4개와 전체 성공 변경으로 검증한 새 INDEPENDENT/PENDING Case를 만들며 Run은 실행하지 않고 Draft revision도 늘리지 않습니다. 같은 requestId/정규화 내용은 같은 작업, 완료 뒤 새 requestId의 같은 내용도 원래 작업/Case를 반환합니다. 진행202·종료200이며 Location의 결과를2초 간격으로 조회하고 SUCCEEDED/FAILED면 중단합니다. 202는 최종저장 성공이 아닙니다. 본문 UTF-8 1MiB·깊이8이며 회사/사용자/저장 경로는 인증/서버에서 정합니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required=true,content=@Content(schema=@Schema(implementation=FinalizationRequestSchema.class),examples=@ExampleObject(value="{\"requestId\":\"aefb60dc-18b8-4ac2-beb4-a4d499950767\",\"baseRevision\":0,\"title\":\"수요 조정 검토\",\"description\":null}")))
    @ApiResponse(responseCode="202",description="SUC-000: 신규 또는 진행 요청. 최종저장 성공 아님",headers={
        @io.swagger.v3.oas.annotations.headers.Header(name="Location",description="statusEndpoint와 같은 상대 경로",schema=@Schema(type="string")),
        @io.swagger.v3.oas.annotations.headers.Header(name="Retry-After",description="다음 조회까지 초",schema=@Schema(type="string",example="2"))},
        content=@Content(schema=@Schema(implementation=FinalizationEnvelope.class),examples=@ExampleObject(value="""
        {"status":true,"code":"SUC-000","message":"Success.","data":{"operationId":"aefb60dc-18b8-4ac2-beb4-a4d499950766","requestId":"aefb60dc-18b8-4ac2-beb4-a4d499950767","draftId":"aefb60dc-18b8-4ac2-beb4-a4d499950765","status":"QUEUED","statusLabel":"대기 중","baseRevision":0,"finalizedCaseId":null,"files":[],"caseEndpoint":null,"failure":null,"statusEndpoint":"/api/v1/mi/fundamental-model/input-drafts/aefb60dc-18b8-4ac2-beb4-a4d499950765/finalizations/aefb60dc-18b8-4ac2-beb4-a4d499950766"}}
        """)))
    @ApiResponse(responseCode="200",description="SUC-000: 종료 요청 또는 완료 재클릭의 원래 결과",headers=@io.swagger.v3.oas.annotations.headers.Header(name="Location",description="statusEndpoint와 같은 상대 경로",schema=@Schema(type="string")),content=@Content(schema=@Schema(implementation=FinalizationEnvelope.class)))
    @ApiResponse(responseCode="400",description="REQ-003/REQ-005: 잘못된 JSON/key/타입. 제목·설명 오류 위치는 /title·/description",content=@Content(schema=@Schema(implementation=ProblemEnvelope.class)))
    @ApiResponse(responseCode="404",description="RSC-001: 본인 Draft/접근 가능한 원본 없음. data:null",content=@Content(schema=@Schema(implementation=ProblemEnvelope.class)))
    @ApiResponse(responseCode="409",description="BIZ-001: IDEMPOTENCY_CONFLICT, DRAFT_BUSY, REVISION_CONFLICT, DRAFT_NOT_EDITABLE, SOURCE_CHANGED, POLICY_INCOMPLETE/POLICY_UNSUPPORTED",content=@Content(schema=@Schema(implementation=ProblemEnvelope.class)))
    @ApiResponse(responseCode="413",description="REQ-003: PAYLOAD_TOO_LARGE",content=@Content(schema=@Schema(implementation=ProblemEnvelope.class)))
    @ApiResponse(responseCode="500",description="SYS-001: 내부 처리/저장 결과 불명. 같은 requestId로 확인. data:null",content=@Content(schema=@Schema(implementation=ProblemEnvelope.class)))
    @RequiredPermissionRules(rules={@PermissionRule(role=Role.USER,conditions={PermissionCondition.SKGAS_COMPANY_USER_ONLY}),@PermissionRule(role=Role.ADMIN,conditions={PermissionCondition.THIRD_PARTY_ADMIN_ONLY})})
    public ResponseEntity<byte[]> finalizeDraft(@Parameter(description="본인 Draft ID",schema=@Schema(format="uuid")) @PathVariable String draftId,HttpServletRequest request)throws IOException {
        var view=service.submitFinalization(uuid(draftId),json.finalizationRequest(request.getInputStream()));
        boolean pending=view.status().equals("QUEUED")||view.status().equals("RUNNING");
        var response=ResponseEntity.status(pending?202:200).header("Location",view.statusEndpoint());
        if(pending)response.header("Retry-After","2");
        return response.contentType(MediaType.APPLICATION_JSON).body(json.bytes(BaseResponseFactory.create(view)));
    }

    @GetMapping("/input-drafts/{draftId}/finalizations/{operationId}")
    @Operation(summary="Draft 최종저장 결과 조회",description="접수 응답 유실 또는 처리 중인 FINALIZE 작업을 조회합니다. 진행/성공/실패 모두 HTTP200/SUC-000입니다. QUEUED/RUNNING은2초 간격으로 조회하고 SUCCEEDED/FAILED면 종료합니다. SUCCEEDED만 finalizedCaseId·files4·caseEndpoint를 제공하며 새 Case 조회로 이동합니다. FAILED는 직전 성공 Draft를 유지하고 failure만 제공합니다. 같은 요청은 재실행하지 않으며 재시도는 현재 Draft 확인 뒤 새 requestId를 사용합니다. CHANGE ID와 다른 소유자/원본 접근 불가는404입니다.")
    @ApiResponse(responseCode="200",description="SUC-000: 기록된 결과. FAILED도 조회 성공",content=@Content(schema=@Schema(implementation=FinalizationEnvelope.class),examples=@ExampleObject(value="""
        {"status":true,"code":"SUC-000","message":"Success.","data":{"operationId":"aefb60dc-18b8-4ac2-beb4-a4d499950766","requestId":"aefb60dc-18b8-4ac2-beb4-a4d499950767","draftId":"aefb60dc-18b8-4ac2-beb4-a4d499950765","status":"SUCCEEDED","statusLabel":"최종저장 완료","baseRevision":1,"finalizedCaseId":104,"files":[{"role":"SUPPLY","fileNo":501},{"role":"DEMAND","fileNo":502},{"role":"TRADE","fileNo":503},{"role":"OUTPUT","fileNo":504}],"caseEndpoint":"/api/v1/mi/fundamental-model/cases/104","failure":null,"statusEndpoint":"/api/v1/mi/fundamental-model/input-drafts/aefb60dc-18b8-4ac2-beb4-a4d499950765/finalizations/aefb60dc-18b8-4ac2-beb4-a4d499950766"}}
        """)))
    @ApiResponse(responseCode="400",description="REQ-003: 잘못된 UUID",content=@Content(schema=@Schema(implementation=ProblemEnvelope.class)))
    @ApiResponse(responseCode="404",description="RSC-001: 본인 Draft/FINALIZE 작업·접근 가능한 원본 없음. data:null",content=@Content(schema=@Schema(implementation=ProblemEnvelope.class)))
    @ApiResponse(responseCode="500",description="SYS-001: 내부 저장/판독 실패. data:null",content=@Content(schema=@Schema(implementation=ProblemEnvelope.class)))
    @RequiredPermissionRules(rules={@PermissionRule(role=Role.USER,conditions={PermissionCondition.SKGAS_COMPANY_USER_ONLY}),@PermissionRule(role=Role.ADMIN,conditions={PermissionCondition.THIRD_PARTY_ADMIN_ONLY})})
    public ResponseEntity<byte[]> finalizationOperation(@Parameter(description="본인 Draft ID",schema=@Schema(format="uuid")) @PathVariable String draftId,
            @Parameter(description="FINALIZE 작업 ID",schema=@Schema(format="uuid")) @PathVariable String operationId) {
        return success(service.finalizationOperation(uuid(draftId),uuid(operationId)));
    }

    private ResponseEntity<byte[]> success(Object value) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json.bytes(BaseResponseFactory.create(value)));
    }

    @ExceptionHandler(InputDraftException.class)
    public ResponseEntity<byte[]> failure(InputDraftException ex, HttpServletRequest request) {
        Object data = ex.getReason() == null || request.getRequestURI().endsWith("/inputs/structure")
            ? null : new Problem(ex.getReason(), ex.getRevision(), ex.getIssues());
        return ResponseEntity.status(ex.getStatus()).contentType(MediaType.APPLICATION_JSON)
            .body(json.bytes(BaseResponseFactory.create(ex.getCode(), data)));
    }

    // Provider/SQL/parse 예외의 원문에는 셀 값·저장소 주소가 포함될 수 있다.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<byte[]> unexpected(Exception ex, HttpServletRequest request) {
        if (ex instanceof CustomAuthorizationException denied) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON)
                .body(json.bytes(BaseResponseFactory.create(denied.getErrorCode())));
        }
        if (ex instanceof com.sk.common.exception.AppErrorException invalid) {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                .body(json.bytes(BaseResponseFactory.create(invalid.getErrorCode())));
        }
        return failure(InputDraftException.internal(), request);
    }

    private static String uuid(String value) {
        if (value == null || !value.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) throw InputDraftException.invalid();
        return value;
    }
    private static long positive(String value) {
        long result = nonnegative(value);
        if (result == 0) throw InputDraftException.invalid();
        return result;
    }
    private static long nonnegative(String value) {
        if (value == null || !value.matches("0|[1-9][0-9]{0,18}")) throw InputDraftException.invalid();
        try { return Long.parseLong(value); } catch (NumberFormatException ex) { throw InputDraftException.invalid(); }
    }
}
