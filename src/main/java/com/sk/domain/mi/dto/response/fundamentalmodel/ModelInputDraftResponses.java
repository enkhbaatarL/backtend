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
        @Schema(description="고정한 원본 Case ID",format="int64",minimum="1",requiredMode=Schema.RequiredMode.REQUIRED) long sourceCaseId,
        @Schema(description="회사·Case 비교 키",pattern="^sc1_[0-9a-f]{64}$",requiredMode=Schema.RequiredMode.REQUIRED) String sourceKey,
        @Schema(description="고정 원본4파일·발행 기준",pattern="^sv1_[0-9a-f]{64}$",requiredMode=Schema.RequiredMode.REQUIRED) String sourceVersion,
        @Schema(description="Draft 편집 상태",allowableValues={"DRAFT","FINALIZED","DISCARDED"},requiredMode=Schema.RequiredMode.REQUIRED) String status,
        @Schema(description="편집 상태 표시명",requiredMode=Schema.RequiredMode.REQUIRED) String statusLabel,
        @Schema(description="현재 성공 저장 번호",minimum="0",maximum="9007199254740991",requiredMode=Schema.RequiredMode.REQUIRED) long revision,
        @Schema(description="진행 요청 ID. 없으면 null",nullable=true,types={"string","null"},requiredMode=Schema.RequiredMode.REQUIRED) String activeOperationId,
        @Schema(description="진행 요청 조회 경로. 없으면 null",nullable=true,types={"string","null"},requiredMode=Schema.RequiredMode.REQUIRED) String activeOperationEndpoint,
        @Schema(description="최종저장 성공 Case. 미완료는 null",nullable=true,types={"integer","null"},requiredMode=Schema.RequiredMode.REQUIRED) Long finalizedCaseId,
        @Schema(description="Draft 구조 재조회 상대 경로",requiredMode=Schema.RequiredMode.REQUIRED) String draftEndpoint) {}
    public record Created(@JsonUnwrapped Summary summary,
        @Schema(description="신규 생성 true, 기존 재개 false",requiredMode=Schema.RequiredMode.REQUIRED) boolean created) {}
    public record Structure(@JsonUnwrapped Summary summary,
        @Schema(description="동일 성공 기준의 네 역할 구조",requiredMode=Schema.RequiredMode.REQUIRED) List<Section> sections,
        @Schema(description="표 미선택이므로 항상 null",nullable=true,types={"null"},requiredMode=Schema.RequiredMode.REQUIRED) Object tableData) {}
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
        @Schema(description="전체 본문 행 수. 빈 슬롯 포함",minimum="0") int totalBodyRows,
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
    public record Editing(Map<String,String> capabilities,List<JsonNode> fields,List<RowBinding> rowBindings,List<EditValue> editValues) {}
    @Schema(requiredProperties={"draftId","revision","sourceKey","sourceVersion","tableData","page","editing"})
    public record Table(@Schema(format="uuid") String draftId,
        @Schema(description="조회한 성공 저장 번호",minimum="0",maximum="9007199254740991") long revision,
        String sourceKey,String sourceVersion,TableData tableData,Page page,Editing editing) {}
    @Schema(requiredProperties={"reason","currentRevision","issues"})
    public record Problem(@Schema(description="클라이언트 분기용 오류 사유") String reason,
        @Schema(description="현재 성공 저장 번호. 판단할 수 없으면 null",nullable=true,types={"integer","null"},minimum="0",maximum="9007199254740991") Long currentRevision,
        @Schema(description="명령별 상세 오류. 이 조회 slice는 빈 배열") List<Object> issues) {}
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
