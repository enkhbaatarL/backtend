package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.annotation.UserResolve;
import com.sk.common.exception.ApiErrorException;
import com.sk.common.exception.CustomAuthorizationException;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.request.agent.ChatRoomReq;
import com.sk.domain.mi.dto.request.agent.QnaAssessmentReq;
import com.sk.domain.mi.dto.request.agent.QnaReq;
import com.sk.domain.mi.dto.request.agent.StreamCancelReq;
import com.sk.domain.mi.dto.response.agent.ChatRoomRes;
import com.sk.domain.mi.dto.response.agent.MajorQuestionRes;
import com.sk.domain.mi.dto.response.agent.QnaRes;
import com.sk.domain.mi.dto.response.agent.StreamResponseWrapper;
import com.sk.domain.mi.exception.AgentStreamException;
import com.sk.domain.mi.service.agent.MariAgentService;
import com.sk.domain.mi.service.agent.stream.SseConnectionManager;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

@Profile("!dedicated")
@Tag(name = "11. Mari Agent API", description = "AI Agent 채팅 서비스 Mari API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/mari-agent")
@RequiredArgsConstructor
public class MiMariAgentController {

    private final MariAgentService mariAgentService;
    private final AuthorizationService authorizationService;
    private final SseConnectionManager sseConnectionManager;
    @Value("${api.model-agent.read-timeout}")
    private Duration sseTimeout;

    @ApiOrder(1)
    @Operation(summary = "주요 질의문 목록 조회 API", description = "분과 별 주요 질의문 목록을 조회합니다. <br>목록은 `questionRank` 필드를 기반으로 오름차순 정렬됩니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/major-questions")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<List<MajorQuestionRes>> getMajorQuestions() {
        return BaseResponseFactory.create(mariAgentService.getMajorQuestions());
    }

    @ApiOrder(2)
    @Operation(summary = "채팅룸 목록 조회 API", description = "로그인된 사용자의 Mari 에이전트 채팅룸 목록을 조회합니다. <br>목록은 생성일 필드를 기반으로 내림차순 정렬됩니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/chat-rooms")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<List<ChatRoomRes>> getChatRooms(@UserResolve User user) {
        return BaseResponseFactory.create(mariAgentService.getChatRooms(user));
    }

    @ApiOrder(3)
    @Parameter(name = "chatRoomId", description = "채팅룸 ID")
    @Operation(summary = "채팅룸 수정 API", description = "채팅룸을 수정합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PatchMapping("/chat-rooms/{chatRoomId}")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<ChatRoomRes> patchChatRoom(
        @PathVariable(value = "chatRoomId") UUID chatRoomId,
        @RequestBody @Valid ChatRoomReq chatRoomReq,
        @UserResolve User user
    ) {
        return BaseResponseFactory.create(
            mariAgentService.patchChatRoom(chatRoomId, chatRoomReq, user));
    }

    @ApiOrder(4)
    @Parameter(name = "chatRoomId", description = "채팅룸 ID")
    @Operation(summary = "채팅룸 삭제 API", description = "채팅룸을 삭제합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @DeleteMapping("/chat-rooms/{chatRoomId}")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<ChatRoomRes> deleteChatRoom(
        @PathVariable(value = "chatRoomId") UUID chatRoomId,
        @UserResolve User user
    ) {
        return BaseResponseFactory.create(mariAgentService.deleteChatRoom(chatRoomId, user));
    }

    @ApiOrder(5)
    @Parameter(name = "chatRoomId", description = "채팅룸 ID (UUID)")
    @Parameter(name = "page", description = "페이지 번호 (기본값: 1)")
    @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 3)")
    @Operation(summary = "채팅룸 QnA 목록 조회 API (with pagination)", description = "로그인된 사용자의 Mari 에이전트 특정 채팅룸의 질의응답(QnA) 목록을 페이지네이션으로 조회합니다. <br>목록은 최신순으로 정렬됩니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/chat-rooms/{chatRoomId}/qna")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<PaginationRes<QnaRes>> getChatRoomQnaList(
        @PathVariable(value = "chatRoomId") UUID chatRoomId,
        @RequestParam(value = "page", required = false, defaultValue = "1") @Min(1) int page,
        @RequestParam(value = "size", required = false, defaultValue = "3") int size,
        @UserResolve User user
    ) {
        return BaseResponseFactory.create(
            mariAgentService.getChatRoomQnaList(page, size, chatRoomId, user));
    }

    @ApiOrder(6)
    @Parameter(name = "chatRoomId", description = "채팅룸 ID (UUID)")
    @Operation(summary = "질의 API", description = "Mari 에이전트에게 질의합니다. <br>새로운 채팅룸에서 대화를 시작하려면 `chatRoomId`를 UUID로 생성해서 전달합니다. 존재하지 않는 `chatRoomId` 전달 시, 해당 ID로 채팅룸을 생성하고 첫 번째 QnA로 등록합니다. <br>기존 채팅룸에서 대화를 이어가려면 기존의 `chatRoomId`를 전달합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/chat-rooms/{chatRoomId}/qna")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<QnaRes> postChatRoomQna(
        @PathVariable(value = "chatRoomId") UUID chatRoomId,
        @RequestBody @Valid QnaReq.Insert qnaReq,
        @UserResolve User user
    ) {
        int companyNo = authorizationService.getCompanyNoWithPriority();
        return BaseResponseFactory.create(
            mariAgentService.postChatRoomQna(chatRoomId, qnaReq, user, companyNo));
    }

    @ApiOrder(7)
    @Parameter(name = "chatRoomId", description = "채팅룸 ID (UUID)")
    @Operation(summary = "질의 Stream API", description = "Mari 에이전트에게 Stream 방식으로 질의합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping(value = "/chat-rooms/{chatRoomId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public SseEmitter postChatRoomStream(
        @PathVariable(value = "chatRoomId") UUID chatRoomId,
        @RequestBody @Valid QnaReq.Insert qnaReq,
        @UserResolve User user
    ) {
        // 26.07.29 취약점 진단 검토, '표준 라이브러리의 널 반환 값 역참조-102277', 오탐 -> sseTimeout은 @Value 주입 필드로 프로퍼티 누락 시 기동 실패하므로 런타임에 null 불가.
        SseEmitter emitter = new SseEmitter(sseTimeout.toMillis());
        log.info("Creating SSE connection - chatRoomId: {}, user: {}", chatRoomId,
            user.getUserNo());

        int companyNo = authorizationService.getCompanyNoWithPriority();

        // StreamResponseWrapper를 받아서 qnaNo와 Flux 분리
        StreamResponseWrapper streamWrapper = mariAgentService.streamChatResponse(chatRoomId,
            qnaReq, user, companyNo);
        Integer qnaNo = streamWrapper.getQnaNo();

        log.info("Stream initialized - chatRoomId: {}, qnaNo: {}", chatRoomId, qnaNo);

        Disposable disposable = streamWrapper.getStream()
            .subscribeOn(Schedulers.boundedElastic())
            .doOnNext(answerRes -> {
                try {
                    emitter.send(SseEmitter.event()
                        .id(answerRes.metadata().node())
                        .name(answerRes.metadata().data())
                        .data(answerRes.message()));
                } catch (IOException e) {
                    log.error("Failed to send stream event - chatRoomId: {}", chatRoomId, e);
                    emitter.completeWithError(e);
                }
            })
            .doOnError(error -> {
                log.error("Stream processing error - chatRoomId: {}", chatRoomId, error);
                sendErrorEvent(emitter, error, String.valueOf(chatRoomId));
                emitter.complete();
            })
            .doOnComplete(() -> {
                log.info("Stream completed successfully - chatRoomId: {}", chatRoomId);
                try {
                    emitter.send(SseEmitter.event()
                        .id("CompleteNode")
                        .name("complete")
                        .data(Map.of(
                            "qnaNo", qnaNo != null ? qnaNo : chatRoomId,
                            "data", "completed"
                        )));
                    emitter.complete();
                } catch (IOException e) {
                    log.error("Failed to send completion event - chatRoomId: {}", chatRoomId, e);
                    emitter.complete();
                }
                sseConnectionManager.removeActiveStream(chatRoomId);
            })
            .subscribe(null, null, () -> sseConnectionManager.removeActiveStream(chatRoomId));

        // qnaNo와 Disposable을 SseConnectionManager에 등록
        if (qnaNo != null) {
            sseConnectionManager.registerActiveStream(chatRoomId, qnaNo, disposable);
        } else {
            log.error("Failed to register stream - qnaNo is null for chatRoomId: {}", chatRoomId);
        }

        emitter.onCompletion(() -> {
                log.info("SSE connection completed - chatRoomId: {}", chatRoomId);
                sseConnectionManager.removeActiveStream(chatRoomId);
            }
        );

        emitter.onTimeout(() -> {
            log.warn("SSE connection timeout - chatRoomId: {}", chatRoomId);
            emitter.complete();
        });

        return emitter;
    }

    /**
     * 에러를 SSE 이벤트로 안전하게 전송
     */
    private void sendErrorEvent(SseEmitter emitter, Throwable error, String chatRoomId) {
        try {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("timestamp", System.currentTimeMillis());

            if (error instanceof ApiErrorException apiError) {
                errorData.put("error", "VALIDATION_ERROR");
                errorData.put("code", apiError.getStatusCode());
                errorData.put("message", apiError.getMessage());

            } else if (error instanceof CustomAuthorizationException authError) {
                errorData.put("error", "AUTHORIZATION_ERROR");
                errorData.put("code", authError.getErrorCode());
                errorData.put("message", authError.getMessage());

            } else if (error instanceof AgentStreamException agentStreamError) {
                errorData.put("error", "STREAM_ERROR");
                errorData.put("statusCode", agentStreamError.getStatusCode());
                errorData.put("message", agentStreamError.getMessage());

            } else {
                errorData.put("error", error.getClass().getSimpleName());
                errorData.put("message", error.getMessage());
            }

            emitter.send(SseEmitter.event()
                .name("error")
                .data(errorData));

            log.info("Sent error event - chatRoomId: {}, error: {}",
                chatRoomId, errorData.get("error"));

        } catch (IOException e) {
            log.error("Failed to send error event - chatRoomId: {}", chatRoomId, e);
        }
    }

    @ApiOrder(8)
    @Parameter(name = "chatRoomId", description = "채팅룸 ID")
    @Parameter(name = "qnaNo", description = "질의응답 번호")
    @Operation(summary = "채팅룸 QnA 평가 API", description = "Mari 에이전트의 응답을 평가합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PatchMapping("/chat-rooms/{chatRoomId}/qna/{qnaNo}/assessment")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<?> patchQnaAssessment(
        @PathVariable(value = "chatRoomId") UUID chatRoomId,
        @PathVariable(value = "qnaNo") Integer qnaNo,
        @RequestBody @Valid QnaAssessmentReq qnaAssessmentReq
    ) {
        return BaseResponseFactory.create(
            mariAgentService.patchQnaAssessment(chatRoomId, qnaNo, qnaAssessmentReq));
    }

    @ApiOrder(9)
    @Parameter(name = "chatRoomId", description = "채팅룸 ID")
    @Parameter(name = "qnaNo", description = "질의응답 번호")
    @Operation(summary = "채팅룸 QnA 답변 생성 재시도 API", description = "Mari 에이전트의 응답을 재생성합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/chat-rooms/{chatRoomId}/qna/{qnaNo}/retry")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<?> postQnaRetry(
        @PathVariable(value = "chatRoomId") UUID chatRoomId,
        @PathVariable(value = "qnaNo") Integer qnaNo,
        @UserResolve User user
    ) {
        int companyNo = authorizationService.getCompanyNoWithPriority();
        return BaseResponseFactory.create(
            mariAgentService.postQnaRetry(chatRoomId, qnaNo, user, companyNo));
    }

    /**
     * 질의 Stream 중단 API
     * <p>
     * 동작 흐름: 1. 활성 스트림 확인 2. 스트림 중단 (dispose 호출) 3. doFinally에서 자동으로 DB 저장 4. 클라이언트에게 응답 반환
     * <p>
     * 주의사항: - DB 저장은 doFinally에서 비동기로 수행됨 - 저장 실패 시 로그에만 기록되며 응답에는 영향 없음 - 중복 취소 요청은 자동으로 무시됨
     */
    @ApiOrder(10)
    @Operation(
        summary = "질의 Stream 중단 API",
        description = """
            특정 채팅룸에서 진행 중인 Stream 질의응답 작업을 중단합니다.

            - 중단 상태(CANCELLED_BY_USER)는 자동으로 DB에 저장됩니다
            - 이미 중단되었거나 완료된 스트림에 대한 요청은 무시됩니다
            """
    )
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PatchMapping("/chat-rooms/{chatRoomId}/stream-cancel")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<Map<String, Object>> cancelMariAgentStream(
        @PathVariable(value = "chatRoomId") UUID chatRoomId,
        @RequestBody(required = false) StreamCancelReq streamCancelReq,
        @UserResolve User user
    ) {
        log.info("Stream cancellation requested - chatRoomId: {}, user: {}",
            chatRoomId, user.getUserNo());

        // 1. 취소 메시지 추출
        String message = streamCancelReq != null ? streamCancelReq.message() : null;
        String cancelMessage = message != null ? message : "canceled_by_user";

        // 2. 스트림 중단 시도
        SseConnectionManager.StreamContext context =
            sseConnectionManager.cancelActiveStream(chatRoomId, cancelMessage);

        // 3. 스트림이 없거나 이미 중단된 경우
        if (context == null) {
            log.warn("No active stream to cancel - chatRoomId: {}", chatRoomId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "No active Mari Agent stream to cancel.");
            result.put("qnaNo", null);
            // 26.07.29 취약점 진단 검토, '누락된 널 값 검사-102182', 오탐 -> chatRoomId는 필수 PathVariable로 Spring이 not null 보장.
            result.put("chatRoomId", chatRoomId.toString());

            return BaseResponseFactory.create(result);
        }

        // 4. 스트림 중단 성공
        Integer qnaNo = context.getQnaNo();

        log.info("Stream cancellation initiated - chatRoomId: {}, qnaNo: {}, message: {}",
            chatRoomId, qnaNo, cancelMessage);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Mari Agent Stream operation cancelled successfully.");
        result.put("qnaNo", qnaNo);
        result.put("chatRoomId", chatRoomId.toString());

        // DB 저장은 doFinally에서 비동기로 처리됨
        // - SignalType.CANCEL 감지 시 updateQnaStatusOnCancel() 호출
        // - 저장 실패는 로그에만 기록되며 응답에는 영향 없음

        return BaseResponseFactory.create(result);
    }

}
