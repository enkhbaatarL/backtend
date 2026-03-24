package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.request.newsletter.NewsletterMailTriggerReq;
import com.sk.domain.mi.service.MiNewsletterPublishService;
import com.sk.domain.mi.service.newsletter.NewsletterIssueOriginalService;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Airflow DAG 등 외부 시스템에서 뉴스레터 메일 발송을 트리거하는 컨트롤러.
 * OAuth2 인증 필요 (Role.SYSTEM 또는 관리자 권한).
 */
@Tag(name = "13-5. Newsletter Mail API", description = "뉴스레터 Mail API입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/newsletter")
@RequiredArgsConstructor
public class MiNewsletterTriggerController {

    private final MiNewsletterPublishService miNewsletterPublishService;
    private final NewsletterIssueOriginalService newsletterIssueOriginalService;

    @ApiOrder(2)
    @Parameter(name = "newsletterId", description = "뉴스레터 마스터 ID")
    @Parameter(name = "issueNo", description = "회차 번호")
    @Operation(
            summary = "뉴스레터 메일 발송 트리거",
            description = """
                    Airflow DAG이 뉴스레터 발행(PUBLISHED) 완료 시 호출합니다.

                    - 차트 캡처(Playwright) + 구독자 개별 메일 발송을 **비동기**로 실행합니다.
                    - 호출 즉시 **202 Accepted** 를 반환하며, 실제 발송은 백그라운드에서 진행됩니다.
                    """)
    @ApiResponse(responseCode = "202", description = "발송 작업이 비동기로 시작됨")
    @PostMapping("/{newsletterId}/{issueNo}/trigger-mail")
    @RequiredPermission(roles = {Role.API_SUB_ADMIN, Role.API_PLUS_OPERATOR, Role.ADMIN, Role.SYSTEM})
    public ResponseEntity<BaseResponse<String>> triggerMail(
            @PathVariable Integer newsletterId,
            @PathVariable Integer issueNo,
            @Valid @RequestBody NewsletterMailTriggerReq request) {

        log.info("Newsletter mail trigger received. newsletterId={}, issueNo={}",
                newsletterId, issueNo);

        newsletterIssueOriginalService.upsertOriginalIssue(newsletterId, issueNo);

        // TODO [newsletter_send_history 설계 연계]:
        //  - trigger 요청 멱등/상태 관리를 위한 별도 테이블(예: newsletter_send_trigger) 추가
        //  - idempotencyKey UNIQUE + status(RECEIVED/PROCESSING/SUCCEEDED/FAILED) 저장
        //  - trigger-mail API 진입 시 중복 요청 여부를 선검증 후 비동기 발송 실행
        miNewsletterPublishService.captureAndSendAsync(
                newsletterId, issueNo, request.subject());

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(BaseResponseFactory.create("accepted"));
    }
}
