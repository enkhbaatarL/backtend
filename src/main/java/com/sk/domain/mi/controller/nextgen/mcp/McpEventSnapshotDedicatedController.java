package com.sk.domain.mi.controller.nextgen.mcp;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.request.nextgen.snapshot.EventSnapshotListReq;
import com.sk.domain.mi.dto.request.nextgen.snapshot.EventSnapshotLookupReq;
import com.sk.domain.mi.dto.request.nextgen.snapshot.EventSnapshotOutlookListReq;
import com.sk.domain.mi.dto.request.nextgen.snapshot.EventSnapshotRecordListReq;
import com.sk.domain.mi.dto.response.nextgen.snapshot.EventSnapshotCompareRes;
import com.sk.domain.mi.dto.response.nextgen.snapshot.EventSnapshotDetailRes;
import com.sk.domain.mi.dto.response.nextgen.snapshot.EventSnapshotLookupRes;
import com.sk.domain.mi.dto.response.nextgen.snapshot.EventSnapshotOutlookRes;
import com.sk.domain.mi.dto.response.nextgen.snapshot.EventSnapshotRecordRes;
import com.sk.domain.mi.dto.response.nextgen.snapshot.EventSnapshotSummaryRes;
import com.sk.domain.mi.service.nextgen.EventSnapshotService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.enums.nextgen.EventSnapshotCompareTarget;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * [MCP Snapshot] Event 버전 이력(Snapshot) 조회 — MCP 전용 (원본 화면 API 없음, 설계 Decision Log D-1).
 * 기존 MCP 이벤트 컨트롤러와 달리 원본 API 위임이 아니고 서비스도 다르므로 분리한다.
 * 인증·X-User-Id 위임·Swagger 그룹은 경로 prefix 로 공통 설정이 자동 적용된다.
 */
@Profile("dedicated")
@Tag(name = "MCP Next Gen MI Event Snapshot API", description = "MCP client용 Next Gen MI 이벤트 버전 이력(Snapshot) 조회 API입니다.")
@RestController
@RequestMapping("/api/v1/mcp/mi/next-gen/events")
@RequiredArgsConstructor
public class McpEventSnapshotDedicatedController {

    private final EventSnapshotService eventSnapshotService;

    @ApiOrder(11)
    @Operation(
        summary = "이벤트 버전 이력 목록 조회",
        description = """
            Event 의 버전 이력(Snapshot) 목록을 버전 번호 내림차순으로 조회합니다. Snapshot 은 파이프라인이 Event 를 생성·갱신한 직후 상태를 저장한 것이며, \
            모든 DB 변경을 기록하지는 않으므로 최신 Snapshot 과 현재 Event(이벤트 상세 조회)가 다를 수 있습니다. \
            changedFields 는 해당 갱신에서 본문 4개 항목(제목·한줄요약·핵심요약·상세분석)의 직전·직후 비교 결과이며, \
            두 버전 간 전체 차이는 버전 비교 API 로 확인합니다. 원본 Event 가 삭제되어도 이력은 조회되며, isEventDeleted 는 저장 당시 Event 상태입니다. \
            목록에는 상세 분석 본문이 없습니다. 시각은 모두 KST 이며 startDate·endDate 도 KST 날짜입니다.""")
    @ApiResponse(responseCode = "200", description = "조회 성공. 이력이 없으면 빈 목록")
    @GetMapping("/{eventId}/snapshots")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<PaginationRes<EventSnapshotSummaryRes>> getEventSnapshots(
        @Parameter(description = "이벤트 ID", example = "1269") @PathVariable Long eventId,
        @Valid @ParameterObject EventSnapshotListReq request
    ) {
        return BaseResponseFactory.create(eventSnapshotService.getEventSnapshots(eventId, request));
    }

    @ApiOrder(12)
    @Operation(
        summary = "이벤트 버전·시점 조회",
        description = """
            버전 번호(versionNo) 또는 기준 시각(asOf)으로 Snapshot 1건을 찾습니다. versionNo 와 asOf 는 동시에 줄 수 없습니다. \
            둘 다 없으면 가장 최근에 저장된 버전입니다. asOf 는 KST 이며, 그 시각까지 저장된 마지막 버전을 반환합니다(저장된 이력 기준). \
            없으면 found=false 입니다. 버전 번호가 없는 과거 Snapshot 은 versionNo 로 찾을 수 없습니다. 현재 상태는 이벤트 상세 조회를 사용합니다.""")
    @ApiResponse(responseCode = "200", description = "조회 성공. 이력이 없으면 found=false")
    @GetMapping("/{eventId}/snapshots/lookup")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<EventSnapshotLookupRes> lookupEventSnapshot(
        @Parameter(description = "이벤트 ID", example = "1269") @PathVariable Long eventId,
        @Valid @ParameterObject EventSnapshotLookupReq request
    ) {
        return BaseResponseFactory.create(eventSnapshotService.lookupEventSnapshot(eventId, request));
    }

    @ApiOrder(13)
    @Operation(
        summary = "이벤트 버전 상세 조회",
        description = """
            특정 버전(Snapshot)에 저장된 Event 본문과 속성을 조회합니다. 그 이벤트의 Snapshot 이 아니면 RSC-001 입니다. \
            isEventDeleted 는 저장 당시 원본 Event 의 삭제 여부이며 이력 자체의 삭제 여부가 아닙니다. 시각은 모두 KST 입니다.""")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{eventId}/snapshots/{snapshotId}")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<EventSnapshotDetailRes> getEventSnapshot(
        @Parameter(description = "이벤트 ID", example = "1269") @PathVariable Long eventId,
        @Parameter(description = "Snapshot ID", example = "12") @PathVariable Long snapshotId
    ) {
        return BaseResponseFactory.create(eventSnapshotService.getEventSnapshot(eventId, snapshotId));
    }

    @ApiOrder(14)
    @Operation(
        summary = "이벤트 버전 비교",
        description = """
            선택한 버전을 이전 버전(target=previous) 또는 현재 Event(target=current)와 비교합니다. \
            differences 는 두 상태를 직접 비교한 결과이고 recordedChangedFields 는 파이프라인이 기록한 값입니다. 둘은 다를 수 있습니다. \
            previous 비교는 previousSnapshotId 기준이며, 없으면 comparable=false 입니다. current 비교에서 현재 Event 가 없으면 comparable=false 입니다. \
            값이 다른 필드만 differences 에 담고 같은 필드는 sameFields 에 이름만 담습니다.""")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{eventId}/snapshots/{snapshotId}/compare")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<EventSnapshotCompareRes> compareEventSnapshot(
        @Parameter(description = "이벤트 ID", example = "1269") @PathVariable Long eventId,
        @Parameter(description = "Snapshot ID", example = "12") @PathVariable Long snapshotId,
        @Parameter(description = "비교 대상 (`previous`: 이전 버전 / `current`: 현재 Event)", schema = @Schema(implementation = EventSnapshotCompareTarget.class, defaultValue = "previous"))
        @RequestParam(defaultValue = "previous") EventSnapshotCompareTarget target
    ) {
        return BaseResponseFactory.create(eventSnapshotService.compareEventSnapshot(eventId, snapshotId, target));
    }

    @ApiOrder(15)
    @Operation(
        summary = "이벤트 버전 근거 레코드 조회",
        description = """
            이 버전을 만들 때 LLM 에 전달된 근거 Record 목록입니다. Event 와 현재 연결된 전체 Record(연관 레코드 조회)와 다릅니다. \
            recordFirstCreated 는 Record 발생 시각이고, 버전 저장 시각은 snapshotCreatedAt 입니다. \
            title·summary·상품 정보는 원본 Record 의 현재 값(식별용)이며 원본이 삭제됐으면 isSourceAvailable=false 입니다. \
            isPermitted 는 effective user 와 서버가 결정한 SKGAS 상품 권한 기준입니다.""")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{eventId}/snapshots/{snapshotId}/records")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<PaginationRes<EventSnapshotRecordRes>> getEventSnapshotRecords(
        @Parameter(description = "이벤트 ID", example = "1269") @PathVariable Long eventId,
        @Parameter(description = "Snapshot ID", example = "12") @PathVariable Long snapshotId,
        @Valid @ParameterObject EventSnapshotRecordListReq request
    ) {
        return BaseResponseFactory.create(eventSnapshotService.getEventSnapshotRecords(eventId, snapshotId, request));
    }

    @ApiOrder(16)
    @Operation(
        summary = "이벤트 버전 기관 전망 조회",
        description = """
            버전 저장 당시 활성 기관 전망 전체의 사본입니다. 같은 전망이 여러 버전에 반복될 수 있고, 현재 기관 전망(기관 전망 조회)과 다를 수 있습니다. \
            viewDate 는 발행일, sourceCreatedAt·sourceModifiedAt 은 원본 행의 DB 관리 시각입니다. 발행일 내림차순으로 정렬합니다.""")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{eventId}/snapshots/{snapshotId}/outlooks")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<PaginationRes<EventSnapshotOutlookRes>> getEventSnapshotOutlooks(
        @Parameter(description = "이벤트 ID", example = "1269") @PathVariable Long eventId,
        @Parameter(description = "Snapshot ID", example = "12") @PathVariable Long snapshotId,
        @Valid @ParameterObject EventSnapshotOutlookListReq request
    ) {
        return BaseResponseFactory.create(eventSnapshotService.getEventSnapshotOutlooks(eventId, snapshotId, request));
    }

}
