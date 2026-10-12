package com.sk.domain.mi.controller.nextgen;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.annotation.UserResolve;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.request.nextgen.EventKeywordSettingReq;
import com.sk.domain.mi.dto.response.nextgen.EventKeywordSettingListRes;
import com.sk.domain.mi.dto.response.nextgen.EventKeywordSettingRes;
import com.sk.domain.mi.service.nextgen.EventKeywordSettingService;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Profile("!dedicated")
@Tag(name = "01-2. Next Gen MI Event Keyword API", description = "Next Gen MI 이벤트 키워드 알림 설정 API입니다.")
@RestController
@RequestMapping("/api/v1/mi/next-gen/event-keywords")
@RequiredArgsConstructor
public class EventKeywordSettingController {

    private final EventKeywordSettingService eventKeywordSettingService;

    @ApiOrder(1)
    @Operation(
            summary = "이벤트 키워드 알림 목록 조회 API",
            description = """
                로그인 사용자가 등록한 이벤트 키워드 알림 목록을 조회합니다. <br>
                Event List 툴바의 알림 버튼으로 여는 **키워드 알림 설정 팝업**의 목록 영역에 사용합니다. <br>
                삭제(해지)한 키워드는 포함되지 않으며, 등록순(오래된 순)으로 내려갑니다.

                <br><br>
                ### 응답 필드
                - `keywordCount`: 등록된 키워드 수 — 팝업의 `등록된 키워드 (n/20)` 중 n
                - `maxKeywordCount`: 등록 가능한 최대 키워드 수 (현재 20) — n이 이 값과 같으면 입력 필드와 [추가] 버튼을 비활성화
                - `keywords`: 등록된 키워드 목록 (등록순)
                  - `keywordSettingId`: 키워드 설정 ID — ON/OFF·삭제 API의 Path 값
                  - `keywordName`: 키워드 (서버에서 정규화된 값)
                  - `isNotificationUse`: 키워드별 알림 ON/OFF 토글 상태
                  - `createdAt`: 등록 일시 (yyyy-MM-dd HH:mm:ss)

                <br><br>
                ### 참고
                - 등록된 키워드가 없으면 `keywords`는 빈 목록입니다.
                """
    )
    @ApiResponse(responseCode = "200", description = "조회 성공 (등록된 키워드가 없으면 빈 목록)")
    @GetMapping
    @RequiredPermissionRules(rules = {
            @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
            @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<EventKeywordSettingListRes> getKeywordSettings(@UserResolve User user) {
        return BaseResponseFactory.create(eventKeywordSettingService.getKeywordSettings(user));
    }

    @ApiOrder(2)
    @Operation(
            summary = "이벤트 키워드 알림 등록 API",
            description = """
                이벤트 키워드를 등록합니다. <br>
                등록된 키워드가 포함된 이벤트가 발생하면 알림 메일이 발송됩니다. <br>
                새로 등록한 키워드의 알림은 ON 상태로 시작합니다.

                <br><br>
                ### 요청 필드
                - `keywordName`: 등록할 키워드 / example - 호르무즈, JKM, 카타르 LNG

                <br><br>
                ### 키워드 규칙
                - **정규화**: 서버에서 앞뒤 공백 제거, 연속 공백 1칸, 유니코드 NFC 변환 후 저장합니다.
                  - example - `"  카타르   LNG "` → `"카타르 LNG"`
                - **길이**: 정규화 후 **2자 이상 30자 이하**
                - **개수**: 사용자당 **최대 20개**
                - **중복**: 같은 사용자의 활성 키워드와 대소문자·공백 차이만 있는 키워드는 중복으로 봅니다.
                  - example - `LNG`가 있으면 `lng`, ` LNG ` 등록 불가
                - **재등록**: 삭제했던 키워드를 다시 등록하면 기존 설정이 복구되고 알림은 ON으로 돌아갑니다.

                <br><br>
                ### 응답 필드
                - 등록(또는 복구)된 키워드 1건 — 필드는 목록 조회 API의 `keywords` 항목과 동일

                <br><br>
                ### 오류
                | 코드 | 조건 | 메시지 |
                |---|---|---|
                | `REQ-003` | 정규화 후 2자 미만 | 키워드는 2자 이상 입력하세요. |
                | `REQ-003` | 정규화 후 30자 초과 | 키워드는 30자 이하로 입력하세요. |
                | `BIZ-001` | 이미 20개 등록됨 | 키워드는 최대 20개까지 등록할 수 있습니다. |
                | `RSC-003` | 활성 키워드와 중복 | 이미 등록된 키워드입니다. |
                """
    )
    @ApiResponse(responseCode = "200", description = "등록 성공 (삭제했던 키워드면 복구)")
    @PostMapping
    @RequiredPermissionRules(rules = {
            @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
            @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<EventKeywordSettingRes> createKeywordSetting(
            @RequestBody @Valid EventKeywordSettingReq.Insert keywordSetting,
            @UserResolve User user
    ) {
        return BaseResponseFactory.create(eventKeywordSettingService.createKeywordSetting(keywordSetting, user));
    }

    @ApiOrder(3)
    @Operation(
            summary = "이벤트 키워드 알림 ON/OFF API",
            description = """
                키워드별 알림 사용 여부를 변경합니다 (팝업 목록의 토글). <br>
                OFF인 키워드는 이후 새로 발생하는 이벤트와 매칭하지 않으며, 이미 발생한 알림 내역은 그대로 유지됩니다.

                <br><br>
                ### 사용 가능한 파라미터
                 - keywordSettingId: 키워드 설정 ID (Path) / example - 1

                <br><br>
                ### 요청 필드
                - `isNotificationUse`: 알림 사용 여부 / `true` ON, `false` OFF

                <br><br>
                ### 응답 필드
                - 변경된 키워드 1건 — 필드는 목록 조회 API의 `keywords` 항목과 동일

                <br><br>
                ### 오류
                | 코드 | 조건 |
                |---|---|
                | `RSC-001` | 키워드가 없거나, 삭제됐거나, 다른 사용자의 키워드 |
                """
    )
    @ApiResponse(responseCode = "200", description = "변경 성공")
    @PatchMapping("/{keywordSettingId}/notification-use")
    @RequiredPermissionRules(rules = {
            @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
            @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<EventKeywordSettingRes> updateNotificationUse(
            @Parameter(description = "키워드 설정 ID", example = "1") @PathVariable(value = "keywordSettingId") @Min(value = 1) Long keywordSettingId,
            @RequestBody @Valid EventKeywordSettingReq.UpdateNotificationUse notificationUse,
            @UserResolve User user
    ) {
        return BaseResponseFactory.create(eventKeywordSettingService.updateNotificationUse(keywordSettingId, notificationUse, user));
    }

    @ApiOrder(4)
    @Operation(
            summary = "이벤트 키워드 알림 삭제 API",
            description = """
                키워드를 삭제(해지)합니다. <br>
                화면은 호출 전에 확인 팝업을 띄웁니다 — 제목 `키워드 삭제`, 버튼 `[취소] / [삭제]`. <br>
                삭제한 키워드는 이후 알림 메일이 발송되지 않으며, 이미 발생한 알림 내역은 그대로 유지됩니다.

                <br><br>
                ### 사용 가능한 파라미터
                 - keywordSettingId: 키워드 설정 ID (Path) / example - 1

                <br><br>
                ### 응답 필드
                - 삭제된 키워드 1건 — 필드는 목록 조회 API의 `keywords` 항목과 동일

                <br><br>
                ### 참고
                - 삭제는 soft delete입니다. 같은 키워드를 다시 등록하면 기존 설정이 복구됩니다.

                <br><br>
                ### 오류
                | 코드 | 조건 |
                |---|---|
                | `RSC-001` | 키워드가 없거나, 이미 삭제됐거나, 다른 사용자의 키워드 |
                """
    )
    @ApiResponse(responseCode = "200", description = "삭제 성공")
    @DeleteMapping("/{keywordSettingId}")
    @RequiredPermissionRules(rules = {
            @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
            @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<EventKeywordSettingRes> deleteKeywordSetting(
            @Parameter(description = "키워드 설정 ID", example = "1") @PathVariable(value = "keywordSettingId") @Min(value = 1) Long keywordSettingId,
            @UserResolve User user
    ) {
        return BaseResponseFactory.create(eventKeywordSettingService.deleteKeywordSetting(keywordSettingId, user));
    }
}
