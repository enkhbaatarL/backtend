package com.sk.domain.mi.dto.response.fundamentalmodel;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

/** EDT-01~03 및 진입에 필요한 QRY-01의 공개 응답. */
public final class ModelInputDraftResponses {
    private ModelInputDraftResponses() {}
    public record Summary(
        @Schema(description="개인 편집 Draft ID",format="uuid",requiredMode=Schema.RequiredMode.REQUIRED) String draftId,
        @Schema(description="원본 Case ID. 직접 업로드는 null",format="int64",minimum="1",nullable=true,types={"integer","null"},requiredMode=Schema.RequiredMode.REQUIRED) Long sourceCaseId,
        @Schema(description="서버가 제공한 출처 키",requiredMode=Schema.RequiredMode.REQUIRED) String sourceKey,
        @Schema(description="Case 원본 기준. 직접 업로드는 null",nullable=true,types={"string","null"},pattern="^sv1_[0-9a-f]{64}$",requiredMode=Schema.RequiredMode.REQUIRED) String sourceVersion,
        @Schema(description="Draft 편집 상태",allowableValues={"DRAFT","FINALIZED","DISCARDED"},requiredMode=Schema.RequiredMode.REQUIRED) String status,
        @Schema(description="편집 상태 표시명",requiredMode=Schema.RequiredMode.REQUIRED) String statusLabel,
        @Schema(description="현재 성공 저장 번호",minimum="0",maximum="9007199254740991",requiredMode=Schema.RequiredMode.REQUIRED) long revision,
        @Schema(description="진행 요청 ID. 없으면 null",nullable=true,types={"string","null"},requiredMode=Schema.RequiredMode.REQUIRED) String activeOperationId,
        @Schema(description="진행 요청 조회 경로. 없으면 null",nullable=true,types={"string","null"},requiredMode=Schema.RequiredMode.REQUIRED) String activeOperationEndpoint,
        @Schema(description="최종저장 성공 Case. 미완료는 null",nullable=true,types={"integer","null"},requiredMode=Schema.RequiredMode.REQUIRED) Long finalizedCaseId,
        @Schema(description="Draft 구조 재조회 상대 경로",requiredMode=Schema.RequiredMode.REQUIRED) String draftEndpoint) {
        public Summary(String draftId,long sourceCaseId,String sourceKey,String sourceVersion,String status,String statusLabel,long revision,String activeOperationId,String activeOperationEndpoint,Long finalizedCaseId,String draftEndpoint){this(draftId,Long.valueOf(sourceCaseId),sourceKey,sourceVersion,status,statusLabel,revision,activeOperationId,activeOperationEndpoint,finalizedCaseId,draftEndpoint);}
    }
    public record Created(@JsonUnwrapped Summary summary,
        @Schema(description="신규 생성 true, 기존 재개 false",requiredMode=Schema.RequiredMode.REQUIRED) boolean created) {}
    public record Structure(@JsonUnwrapped Summary summary,
        @Schema(description="동일 성공 기준의 연결 완료 역할 구조. 직접 업로드는1~4개",requiredMode=Schema.RequiredMode.REQUIRED) List<Section> sections,
        @Schema(description="표 미선택이므로 항상 null",nullable=true,types={"null"},requiredMode=Schema.RequiredMode.REQUIRED) Object tableData,
        @JsonUnwrapped Readiness readiness) {
        public Structure(Summary summary,List<Section> sections,Object tableData){this(summary,sections,tableData,null);}
    }
    @Schema(requiredProperties={"uploadedKinds","missingKinds","canFinalize"})
    public record Readiness(List<String> uploadedKinds,List<String> missingKinds,boolean canFinalize) {}
    @Schema(requiredProperties={"sourceKey","sourceVersion","sections","tableData"})
    public record SourceStructure(String sourceKey,String sourceVersion,List<Section> sections,
        @Schema(description="원본 구조 최초 조회에서는 항상 null",nullable=true,types={"null"},requiredMode=Schema.RequiredMode.REQUIRED) Object tableData) {}
    @Schema(requiredProperties={"kind","inputId","sheets"})
    public record Section(@Schema(allowableValues={"DEMAND","SUPPLY","TRADE","OUTPUT"}) String kind,String inputId,List<Sheet> sheets) {}
    @Schema(requiredProperties={"sheetName","tables"})
    public record Sheet(String sheetName,List<TableInfo> tables) {}
    @Schema(requiredProperties={"id","kind","sheetName","name","description","location","rowPolicy","columnPolicy","coordinates","controlIds"})
    public record TableInfo(String id,String kind,String sheetName,String name,@Schema(description="표 설명. 미등록이면 null",nullable=true,types={"string","null"}) String description,String location,
        String rowPolicy,String columnPolicy,Coordinates coordinates,List<String> controlIds) {}
    @Schema(requiredProperties={"headerRow","bodyStart"})
    public record Coordinates(int headerRow,int bodyStart) {}
    @Schema(requiredProperties={"field","headerName"})
    public record Column(String field,String headerName) {}
    @Schema(requiredProperties={"tableId","columns","rows","inputCells","inputGuideRules","controls","displayUpdateRules"})
    public record TableData(String tableId,List<Column> columns,List<Map<String,Object>> rows,List<Long> inputCells,
        JsonNode inputGuideRules,List<JsonNode> controls,JsonNode displayUpdateRules) {}
    @Schema(requiredProperties={"totalBodyRows","returnedBodyRows","startRow","endRow","hasNext","nextStartRow"})
    public record Page(
        @Schema(description="표시 범위 전체 본문 행 수. 범위 안의 빈 슬롯과 값 없는 활성 ADDED 행 포함",minimum="0") int totalBodyRows,
        @Schema(description="이번 페이지 본문 행 수. 헤더 제외",minimum="0",maximum="2000") int returnedBodyRows,
        @Schema(description="이번 본문 첫 전역 행. 본문이 없으면 null",nullable=true,types={"integer","null"},minimum="1",maximum="1048576") Integer startRow,
        @Schema(description="이번 본문 마지막 전역 행. 본문이 없으면 null",nullable=true,types={"integer","null"},minimum="1",maximum="1048576") Integer endRow,
        @Schema(description="다음 본문 페이지 존재 여부") boolean hasNext,
        @Schema(description="다음 조회 startRow. 마지막 페이지는 null",nullable=true,types={"integer","null"},minimum="1",maximum="1048576") Integer nextStartRow) {}
    @Schema(requiredProperties={"rowNumber","rowId","origin"})
    public record RowBinding(
        @Schema(description="이번 성공 revision의 전역 행",minimum="1",maximum="1048576") int rowNumber,
        @Schema(description="안정 논리 행 ID. EMPTY_SLOT은 null",nullable=true,types={"string","null"},format="uuid") String rowId,
        @Schema(description="원본 행·추가 행·빈 슬롯 구분",allowableValues={"SOURCE","ADDED","EMPTY_SLOT"}) String origin) {}
    @Schema(requiredProperties={"rowId","values"})
    public record EditValue(String rowId,Map<String,Object> values) {}
    @Schema(requiredProperties={"capabilities","fields","rowBindings","editValues"})
    public record Editing(@Schema(description="SET/CLEAR/ADD_ROW/DELETE_ROW/ADD_COLUMN/DELETE_COLUMN의 READY/INCOMPLETE/NOT_ALLOWED/UNSUPPORTED. READY만 호출 가능") Map<String,String> capabilities,
        @Schema(description="안정 fieldKey별 입력 정의. dataType은 INTEGER/DECIMAL/TEXT/DECIMAL_OR_TEXT. DECIMAL_OR_TEXT는 JSON number와 string을 원래 타입 그대로 주고받으며 실제 빈칸은 CLEAR로 처리. setConstraints.required=true인 TEXT는 빈 문자열·공백 입력 불가. CLEAR 허용 여부는 policyStatus.CLEAR로 표시") List<JsonNode> fields,List<RowBinding> rowBindings,List<EditValue> editValues,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        @Schema(description="ADD_COLUMN READY일 때만 제공하는 headerValues/body 생성 정책") JsonNode columnCreation) {}
    @Schema(requiredProperties={"draftId","revision","sourceKey","sourceVersion","tableData","page","editing"})
    public record Table(@Schema(format="uuid") String draftId,
        @Schema(description="조회한 성공 저장 번호",minimum="0",maximum="9007199254740991") long revision,
        String sourceKey,@Schema(description="Case 원본 기준. 직접 업로드는 null",nullable=true,types={"string","null"}) String sourceVersion,TableData tableData,Page page,Editing editing) {}
    @Schema(requiredProperties={"reason","currentRevision","issues"})
    public record Problem(@Schema(description="클라이언트 분기용 오류 사유") String reason,
        @Schema(description="현재 성공 저장 번호. 판단할 수 없으면 null",nullable=true,types={"integer","null"},minimum="0",maximum="9007199254740991") Long currentRevision,
        @Schema(description="명령별 상세 오류. 이 조회 slice는 빈 배열") List<Object> issues) {}
    @Schema(requiredProperties={"operationId","requestId","draftId","status","statusLabel","baseRevision","resultRevision","rowMappings","columnMappings","affectedTableIds","failure","statusEndpoint"})
    public record OperationView(
        @Schema(description="서버 작업 ID",format="uuid") String operationId,
        @Schema(description="클라이언트 중복 식별 ID",format="uuid") String requestId,
        @Schema(description="소유 Draft ID",format="uuid") String draftId,
        @Schema(description="처리 상태. SUCCEEDED만 임시저장 성공",allowableValues={"QUEUED","RUNNING","SUCCEEDED","FAILED"}) String status,
        @Schema(description="처리 상태 표시명") String statusLabel,
        @Schema(description="접수한 기준 성공 번호",minimum="0",maximum="9007199254740991") long baseRevision,
        @Schema(description="SUCCEEDED에만 기준+1. 다른 상태는 null",nullable=true,types={"integer","null"},minimum="1",maximum="9007199254740991") Long resultRevision,
        @Schema(description="성공한 신규 행 대응. 성공 전/실패는 빈 배열") List<JsonNode> rowMappings,
        @io.swagger.v3.oas.annotations.media.ArraySchema(arraySchema=@Schema(description="성공 신규 열 대응. 진행·실패·추가 없음은 빈 배열"),schema=@Schema(implementation=ColumnMapping.class)) List<JsonNode> columnMappings,
        @Schema(description="성공 후 다시 조회할 표 ID. 성공 전/실패는 빈 배열") List<String> affectedTableIds,
        @Schema(description="FAILED의 안전한 오류. 다른 상태는 null",nullable=true,types={"object","null"},schemaResolution=Schema.SchemaResolution.INLINE) Problem failure,
        @Schema(description="이 작업 결과 조회의 상대 경로") String statusEndpoint) {}
    @Schema(requiredProperties={"commandId","clientColumnKey","tableId","fieldKey","columnState"},additionalProperties=Schema.AdditionalPropertiesValue.FALSE)
    public record ColumnMapping(@Schema(pattern="^[A-Za-z][A-Za-z0-9_-]{0,63}$") String commandId,
        @Schema(pattern="^[A-Za-z][A-Za-z0-9_-]{0,63}$") String clientColumnKey,String tableId,
        @Schema(description="서버가 발급한 안정 열 키",pattern="^[A-Za-z][A-Za-z0-9_-]{0,63}$") String fieldKey,
        @Schema(allowableValues={"ACTIVE","DELETED"}) String columnState) {}
    @Schema(requiredProperties={"status","code","message","data"})
    public record OperationEnvelope(boolean status,String code,String message,OperationView data) {}
    @Schema(requiredProperties={"role","fileNo"})
    public record FinalizedFile(@Schema(allowableValues={"SUPPLY","DEMAND","TRADE","OUTPUT"}) String role,
        @Schema(description="새 Case에 연결한 파일 번호",format="int64",minimum="1") long fileNo) {}
    @Schema(requiredProperties={"status","statusLabel","runId","statusEndpoint","failureReason"})
    public record FinalizationExecution(
        @Schema(description="최초 연결한 모델 실행 상태",allowableValues={"QUEUED","RUNNING","COMPLETED","FAILED"}) String status,
        @Schema(description="모델 실행 상태 표시명") String statusLabel,
        @Schema(description="최초 연결한 실행 ID. 접수 전/접수 실패는 null",nullable=true,types={"string","null"}) String runId,
        @Schema(description="Case의 최신 실행 상태 조회 경로. 명시적 재실행 후에는 최신 Run을 조회한다") String statusEndpoint,
        @Schema(description="FAILED만 안전한 실패 코드. 내부 오류 원문은 제공하지 않음",nullable=true,types={"string","null"},
            allowableValues={"EXECUTION_REQUEST_FAILED","JOB_SUBMIT_FAILED","MODEL_EXECUTION_FAILED"}) String failureReason) {}
    @Schema(requiredProperties={"operationId","requestId","draftId","status","statusLabel","baseRevision","finalizedCaseId","files","caseEndpoint","failure","statusEndpoint","execution"})
    public record FinalizationView(@Schema(format="uuid") String operationId,@Schema(format="uuid") String requestId,@Schema(format="uuid") String draftId,
        @Schema(allowableValues={"QUEUED","RUNNING","SUCCEEDED","FAILED"}) String status,String statusLabel,
        @Schema(minimum="0",maximum="9007199254740991") long baseRevision,
        @Schema(description="SUCCEEDED에만 새 Case ID. 저장 후 BE가 모델 실행을 자동 접수",nullable=true,types={"integer","null"},format="int64",minimum="1") Long finalizedCaseId,
        @Schema(description="성공만 SUPPLY/DEMAND/TRADE/OUTPUT 4건. 그 외 빈 배열") List<FinalizedFile> files,
        @Schema(description="성공 Case 조회 상대 경로. 그 외 null",nullable=true,types={"string","null"}) String caseEndpoint,
        @Schema(description="FAILED만 안전한 오류. 그 외 null",nullable=true,types={"object","null"},schemaResolution=Schema.SchemaResolution.INLINE) Problem failure,
        String statusEndpoint,
        @Schema(description="저장 성공 후 자동 실행 상태. 저장 전/실패 및 과거 자동 실행 미대상 성공 건은 null",nullable=true,types={"object","null"},schemaResolution=Schema.SchemaResolution.INLINE) FinalizationExecution execution) {}
    @Schema(requiredProperties={"status","code","message","data"})
    public record FinalizationEnvelope(boolean status,String code,String message,FinalizationView data) {}
    // 실제 wire wrapper와 같은 구조를 생성 OpenAPI에 명시한다.
    @Schema(requiredProperties={"status","code","message","data"})
    public record CreatedEnvelope(boolean status,String code,String message,Created data) {}
    @Schema(requiredProperties={"status","code","message","data"})
    public record StructureEnvelope(boolean status,String code,String message,Structure data) {}
    @Schema(requiredProperties={"status","code","message","data"})
    public record SourceEnvelope(boolean status,String code,String message,SourceStructure data) {}
    @Schema(requiredProperties={"status","code","message","data"})
    public record TableEnvelope(boolean status,String code,String message,Table data) {}
    @Schema(requiredProperties={"status","code","message","data"})
    public record ProblemEnvelope(@Schema(description="오류 응답이므로 false") boolean status,String code,String message,
        @Schema(description="사유를 제공하는 오류는 Problem. 404/500 등은 null",nullable=true,types={"object","null"},schemaResolution=Schema.SchemaResolution.INLINE) Problem data) {}
    @Schema(requiredProperties={"status","code","message","data"})
    public record NullErrorEnvelope(@Schema(description="오류 응답이므로 false") boolean status,String code,String message,
        @Schema(description="원본 조회 오류에서는 항상 null",nullable=true,types={"null"}) Object data) {}
}
