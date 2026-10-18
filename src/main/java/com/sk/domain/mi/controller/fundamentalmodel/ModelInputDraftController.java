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
@Tag(name = "6. MI Fundamental Model Input Draft API", description = "개인 Draft 생성·재개와 저장된 원본 값 조회")
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
    @Operation(summary = "개인 Draft 구조 재조회", description = "현재 회사·사용자가 소유한 Draft의 구조와 성공 revision을 반환합니다. 다른 소유자의 Draft는 존재 여부를 공개하지 않고 404입니다. tableData는 null입니다. 현재 구현은 revision 0을 지원합니다.")
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
    @Operation(summary = "Draft 선택 표 조회", description = "구조 조회의 revision과 tableId를 보내 저장된 원본 값, 안정 행 ID와 편집 metadata를 조회합니다. 수식을 재계산하지 않습니다. 본문 최대 2,000행과 전체 응답 2MiB 범위에서 행을 나누며 다음 조회는 page.nextStartRow를 사용합니다. 헤더는 매번 포함됩니다. 값 변경 기능은 후속이므로 capability READY는 UNSUPPORTED로 반환합니다.")
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

    private ResponseEntity<byte[]> success(Object value) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json.bytes(BaseResponseFactory.create(value)));
    }

    @ExceptionHandler(InputDraftException.class)
    public ResponseEntity<byte[]> failure(InputDraftException ex, HttpServletRequest request) {
        Object data = ex.getReason() == null || request.getRequestURI().endsWith("/inputs/structure")
            ? null : new Problem(ex.getReason(), ex.getRevision(), List.of());
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
