package com.sk.domain.mi.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.annotation.UserResolve;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.response.user.UserRes;
import com.sk.domain.mi.dto.request.notification.NotificationSettingReq;
import com.sk.domain.mi.dto.response.notification.MiNotificationRes;
import com.sk.domain.mi.dto.response.notification.MiNotificationSettingGoodsDataSourceRes;
import com.sk.domain.mi.dto.response.notification.MiNotificationSettingRes;
import com.sk.domain.mi.dto.response.notification.MiUserNotificationSettingRes;
import com.sk.domain.mi.dto.response.notification.MyNotificationRes;
import com.sk.domain.mi.dto.response.notification.BellNotificationRes;
import com.sk.domain.mi.dto.response.notification.BellNotificationUnreadCountRes;
import com.sk.domain.mi.service.MiNotificationService;
import com.sk.domain.mi.service.notification.bell.BellNotificationService;
import com.sk.domain.mi.service.notification.mynotification.MyNotificationService;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.enums.notification.NotificationSourceType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Profile("!dedicated")
@Tag(name = "06. 알림 관리 API", description = "알림 관리 API 입니다.")
@RestController
@RequestMapping("/api/v1/mi/notification-management")
@RequiredArgsConstructor
@Slf4j
public class MiNotificationController {

	private final MiNotificationService miNotificationService;
	private final BellNotificationService bellNotificationService;
	private final MyNotificationService myNotificationService;

	@RequiredPermission(roles = {Role.USER})
	@ApiOrder(0)
	@Operation(summary = "전체 알림 설정 조회", description = "전체 알림 설정을 조회합니다.")
	@ApiResponse(responseCode = "200", description = "전체 알림 설정 조회 성공")
	@GetMapping("/notification-setting")
	public BaseResponse<MiUserNotificationSettingRes> getUserNotificationSetting(
			@UserResolve User user) {
		return BaseResponseFactory
				.create(miNotificationService.getUserNotificationSetting(user));
	}

	@RequiredPermission(roles = {Role.USER})
	@ApiOrder(1)
	@Operation(summary = "전체 알림 설정 변경", description = "전체 알림 설정을 변경합니다.")
	@ApiResponse(responseCode = "200", description = "전체 알림 설정 변경 성공")
	@PatchMapping("/notification-setting")
	public BaseResponse<UserRes> updateUserNotificationSetting(
			@RequestBody @Valid NotificationSettingReq.UpdateUserSetting notifications,
			@UserResolve User user) {
		return BaseResponseFactory
				.create(miNotificationService.updateUserNotificationSetting(notifications, user));
	}

	@RequiredPermission(roles = {Role.USER})
	@Parameters({
			@Parameter(name = "page", description = "페이지 번호", required = true),
			@Parameter(name = "size", description = "페이지 크기", required = true)
	})
	@ApiOrder(2)
	@Operation(summary = "알림 설정한 정형 데이터 목록 조회", description = "알림 설정한 정형 데이터 목록을 조회합니다.")
	@ApiResponse(responseCode = "200", description = "알림 조회 성공")
	@GetMapping("/notification-settings")
	public BaseResponse<PaginationRes<MiNotificationSettingGoodsDataSourceRes>> getNotificationSettings(
			@RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
			@RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size,
			@RequestParam(value = "search", required = false) String search,
            @UserResolve User user) {
		return BaseResponseFactory
				.create(miNotificationService.getNotificationSettingsGoodsDataSource(page, size, search, user));
	}

	@RequiredPermission(roles = {Role.USER})
	@Parameters({
			@Parameter(name = "goodsDataSourceNo", description = "상품 데이터 소스 번호", required = true),
	})
	@ApiOrder(3)
	@Operation(summary = "알림 설정", description = "알림을 설정합니다.")
	@ApiResponse(responseCode = "200", description = "알림 설정 성공")
	@PostMapping("/goodsdata/{goodsDataSourceNo}/notification-setting")
	public BaseResponse<MiNotificationSettingRes> createNotificationSetting(
			@PathVariable(value = "goodsDataSourceNo") @Min(value = 1) Integer goodsDataSourceNo,
			@RequestBody @Valid NotificationSettingReq.Insert notifications,
			@UserResolve User user,
			BindingResult bindingResult) {

		if (bindingResult.hasErrors()) {
			log.error("알림 설정 오류: {}", bindingResult.getFieldErrors());
			throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, bindingResult.getFieldErrors().get(0).getDefaultMessage());
		}

		return BaseResponseFactory
				.create(miNotificationService.createNotificationSetting(goodsDataSourceNo, notifications, user));
	}

	@RequiredPermission(roles = {Role.USER})
	@Parameters({
			@Parameter(name = "goodsDataSourceNo", description = "상품 데이터 소스 번호(PK)", required = true),
			@Parameter(name = "page", description = "페이지 번호", required = true),
			@Parameter(name = "size", description = "페이지 크기", required = true)
	})
	@ApiOrder(3)
	@Operation(summary = "정형 데이터 알림 설정 리스트 조회", description = "정형 데이터 알림 설정 리스트를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "알림 조회 성공")
	@GetMapping("/goodsdata/{goodsDataSourceNo}/notification-settings")
	public BaseResponse<PaginationRes<MiNotificationSettingRes>> getNotificationSettings(
			@PathVariable(value = "goodsDataSourceNo") @Min(value = 1) Integer goodsDataSourceNo,
			@UserResolve User user,
			@RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
			@RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size) {
		return BaseResponseFactory
				.create(miNotificationService.getNotificationSettings(page, size, goodsDataSourceNo, user));
	}

	@RequiredPermission(roles = {Role.USER})
	@Parameters({
			@Parameter(name = "goodsDataSourceNo", description = "상품 데이터 소스 번호(PK)", required = true),
			@Parameter(name = "notificationSettingNo", description = "알림 설정 번호(PK)", required = true)
	})
	@ApiOrder(4)
	@Operation(summary = "알림 설정 상세 조회", description = "상품 데이터의 알림 설정을 상세조회합니다.")
	@ApiResponse(responseCode = "200", description = "알림 설정 조회 성공")
	@GetMapping("/goodsdata/{goodsDataSourceNo}/notification-settings/{notificationSettingNo}")
	public BaseResponse<MiNotificationSettingRes> getNotificationSetting(
			@PathVariable(value = "goodsDataSourceNo") @Min(value = 1) Integer goodsDataSourceNo,
			@PathVariable(value = "notificationSettingNo") @Min(value = 1) Integer notificationSettingNo,
			@UserResolve User user) {
		return BaseResponseFactory
				.create(miNotificationService.getNotificationSetting(goodsDataSourceNo, notificationSettingNo, user));
	}

	@Parameters({
			@Parameter(name = "goodsDataSourceNo", description = "상품 데이터 소스 번호(PK)", required = true),
			@Parameter(name = "notificationSettingNo", description = "알림 설정 번호(PK)", required = true)
	})
	@ApiOrder(5)
	@Operation(summary = "알림 설정 삭제", description = "알림 설정을 삭제합니다.")
	@ApiResponse(responseCode = "200", description = "알림 설정 삭제 성공")
	@DeleteMapping("/goodsdata/{goodsDataSourceNo}/notification-settings/{notificationSettingNo}")
	public BaseResponse<MiNotificationSettingRes> deleteNotificationSetting(
			@PathVariable(value = "goodsDataSourceNo") @Min(value = 1) Integer goodsDataSourceNo,
			@PathVariable(value = "notificationSettingNo") @Min(value = 1) Integer notificationSettingNo,
			@UserResolve User user) {
		return BaseResponseFactory
				.create(miNotificationService.deleteNotificationSetting(goodsDataSourceNo, notificationSettingNo, user));
	}

	@RequiredPermission(roles = {Role.USER})
	@Parameters({
			@Parameter(name = "page", description = "페이지 번호", required = true),
			@Parameter(name = "size", description = "페이지 크기", required = true)
	})
	@ApiOrder(6)
	@Operation(summary = "알림 발송 내역 리스트 조회", description = "알림 발송 내역 리스트를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "알림 발송 내역 조회 성공")
	@GetMapping("/notifications")
	public BaseResponse<PaginationRes<MiNotificationRes>> getNotifications(
			@UserResolve User user,
			@RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
			@RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size) {
		return BaseResponseFactory
				.create(miNotificationService.getNotifications(page, size, user));
	}

	@RequiredPermission(roles = {Role.USER})
	@Parameters({
			@Parameter(name = "emailSendSn", description = "알림 발송 내역 번호(PK)", required = true)
	})
	@ApiOrder(7)
	@Operation(summary = "알림 발송 내역 상세보기", description = "알림 발송 내역 상세보기를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "알림 발송 내역 상세보기 조회 성공")
	@GetMapping("/notifications/{emailSendSn}")
	public BaseResponse<MiNotificationRes> getNotification(
			@PathVariable(value = "emailSendSn") @Min(value = 1) Integer emailSendSn,
			@UserResolve User user) {
		return BaseResponseFactory
				.create(miNotificationService.getNotification(emailSendSn, user));
	}

	@RequiredPermission(roles = {Role.USER})
	@ApiOrder(8)
	@Operation(summary = "알림 내역 읽음 처리", description = "알림 내역 읽음 처리를 합니다.")
	@ApiResponse(responseCode = "200", description = "알림 내역 읽음 처리 성공")
	@GetMapping("/notifications/{emailSendSn}/inquiry")
	public BaseResponse<MiNotificationRes> inquiryNotification(
			@PathVariable(value = "emailSendSn") @Min(value = 1) Integer emailSendSn,
			@UserResolve User user) {
		return BaseResponseFactory
				.create(miNotificationService.inquiryNotification(emailSendSn, user));
	}

	@RequiredPermission(roles = {Role.USER})
	@ApiOrder(9)
	@Operation(summary = "알림벨 목록 조회", description = """
			알림벨 알림 목록입니다. 최근 7일, 발생 일시 최신순입니다. <br>
			상품 임계치 알림은 모든 사용자, 이벤트 키워드 알림은 SKGAS 사용자에게 함께 조회됩니다.

			<br><br>
			### 항목 필드
			- `notificationType`: `GOODS_THRESHOLD`(상품 임계치) / `EVENT_KEYWORD`(이벤트 키워드)
			- `notificationId`: 유형별 알림 ID — 읽음 처리 API에 `notificationType`과 함께 사용
			- `notifiedAt` / `isRead`: 발생 일시 / 읽음 여부
			- `goodsThreshold`: 상품 임계치 알림 — 기존 알림 발송 내역 목록 응답과 동일 (그 외 유형은 null)
			- `eventKeyword`: 이벤트 키워드 알림 — `keywordSettingId`, `keywordName`, `eventId`, `eventName` (그 외 유형은 null)

			<br><br>
			### 대체 API
			- 기존 `GET /notifications` (알림 발송 내역 리스트 조회)
			- 기존 목록 항목은 `goodsThreshold`에 그대로 담김 (필드·값 동일, 7일 기준 동일)
			""")
	@ApiResponse(responseCode = "200", description = "알림벨 목록 조회 성공")
	@GetMapping("/bell-notifications")
	public BaseResponse<PaginationRes<BellNotificationRes>> getBellNotifications(
			@UserResolve User user,
			@Parameter(description = "페이지 번호", example = "1") @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
			@Parameter(description = "페이지 크기", example = "10") @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size
    ) {
		return BaseResponseFactory.create(bellNotificationService.getBellNotifications(page, size, user));
	}

	@RequiredPermission(roles = {Role.USER})
	@ApiOrder(10)
	@Operation(summary = "알림벨 미조회 수 조회", description = """
			알림벨의 최근 7일 미조회 알림 수입니다. (벨 아이콘 표시)

			<br><br>
			### 대체 API
			- 신규 (기존 전용 API 없음)
			- 상품 임계치는 기존 `GET /notifications` 목록의 `isEmailInquiry = false` 건수와 같음
			""")
	@ApiResponse(responseCode = "200", description = "알림벨 미조회 수 조회 성공")
	@GetMapping("/bell-notifications/unread-count")
	public BaseResponse<BellNotificationUnreadCountRes> getBellNotificationUnreadCount(@UserResolve User user) {
		return BaseResponseFactory.create(bellNotificationService.getUnreadCount(user));
	}

	@RequiredPermission(roles = {Role.USER})
	@ApiOrder(11)
	@Operation(summary = "알림벨 읽음 처리", description = """
			알림을 읽음 처리하고 해당 항목을 반환합니다. 본인 알림이 아니거나 없으면 400(RSC-001)입니다.

			<br><br>
			### 대체 API
			- `GOODS_THRESHOLD`: 기존 `GET /notifications/{emailSendSn}/inquiry` (알림 내역 읽음 처리)
			  - `notificationId` = `emailSendSn`, 같은 읽음 값을 갱신하므로 기존 API 조회 결과에도 반영됨
			- `EVENT_KEYWORD`: 신규
			""")
	@ApiResponse(responseCode = "200", description = "알림벨 읽음 처리 성공")
	@PatchMapping("/bell-notifications/{notificationType}/{notificationId}/read")
	public BaseResponse<BellNotificationRes> readBellNotification(
			@Parameter(description = "알림 유형", example = "EVENT_KEYWORD") @PathVariable(value = "notificationType") NotificationSourceType notificationType,
			@Parameter(description = "알림 ID", example = "1234") @PathVariable(value = "notificationId") @Min(value = 1) Long notificationId,
			@UserResolve User user
    ) {
		return BaseResponseFactory.create(bellNotificationService.markRead(notificationType, notificationId, user));
	}

	@RequiredPermission(roles = {Role.USER})
	@ApiOrder(12)
	@Operation(summary = "My Notifications 목록 조회", description = """
			알림 설정한 정형 데이터와 이벤트 키워드 알림 목록입니다. 최근 등록·발생 일시 최신순입니다. <br>
			상품 임계치는 모든 사용자, 이벤트 키워드는 SKGAS 사용자에게 함께 조회됩니다.

			<br><br>
			### 항목 필드
			- `notificationType`: `GOODS_THRESHOLD`(상품 임계치) / `EVENT_KEYWORD`(이벤트 키워드)
			- `goodsThreshold`: 알림 설정한 정형 데이터 목록 응답과 동일 (그 외 유형은 null)
			- `eventKeyword`: `keywordSettingId`, `keywordName`, `eventId`, `eventName` (그 외 유형은 null)

			<br><br>
			### 대체 API
			- 기존 `GET /notification-settings` (알림 설정한 정형 데이터 목록 조회)
			- 기존 목록 항목은 `goodsThreshold`에 그대로 담김 (필드·값·검색 조건 동일). 정렬은 최근 설정 등록 일시 순
			- 행 클릭 시 호출은 기존과 동일..
			  - `GOODS_THRESHOLD`: `GET /goodsdata/{goodsDataSourceNo}/notification-settings`, `GET /notification-setting`
			  - `EVENT_KEYWORD`: 이벤트 상세 화면으로 이동 (`eventId`)
			""")
	@ApiResponse(responseCode = "200", description = "My Notifications 목록 조회 성공")
	@GetMapping("/my-notifications")
	public BaseResponse<PaginationRes<MyNotificationRes>> getMyNotifications(
			@UserResolve User user,
			@Parameter(description = "검색어 (상품: 데이터명, 키워드: 이벤트명)") @RequestParam(value = "search", required = false) String search,
			@Parameter(description = "페이지 번호", example = "1") @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
			@Parameter(description = "페이지 크기", example = "10") @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size
    ) {
		return BaseResponseFactory.create(myNotificationService.getMyNotifications(page, size, search, user));
	}

}
