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
import com.sk.domain.mi.dto.response.agent.ChatRoomRes;
import com.sk.domain.mi.dto.response.agent.MajorQuestionRes;
import com.sk.domain.mi.dto.response.agent.QnaRes;
import com.sk.domain.mi.exception.AgentStreamException;
import com.sk.domain.mi.service.agent.MariAgentService;
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
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Profile("!dedicated")
@Tag(name = "11. Mari Agent API", description = "AI Agent 채팅 서비스 Mari API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/mari-agent")
@RequiredArgsConstructor
public class MiMariAgentController {

    private final MariAgentService mariAgentService;
    @Qualifier("sseExecutor")
    private final ExecutorService executor;
    private final AuthorizationService authorizationService;
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
        return BaseResponseFactory.create(mariAgentService.patchChatRoom(chatRoomId, chatRoomReq, user));
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
        return BaseResponseFactory.create(mariAgentService.getChatRoomQnaList(page, size, chatRoomId, user));
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
        return BaseResponseFactory.create(mariAgentService.postChatRoomQna(chatRoomId, qnaReq, user, companyNo));
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
        SseEmitter emitter = new SseEmitter(sseTimeout.toMillis());
        log.info("Creating SSE connection - chatRoomId: {}, user: {}", chatRoomId,
            user.getUserNo());

        int companyNo = authorizationService.getCompanyNoWithPriority();

        Flux.defer(() ->
                mariAgentService.streamChatResponse(chatRoomId, qnaReq, user, companyNo)
            )
            .subscribeOn(Schedulers.boundedElastic())
            .doOnNext(answerRes -> executor.execute(() -> {
                try {
                    emitter.send(SseEmitter.event()
                        .id(answerRes.metadata().node())
                        .name(answerRes.metadata().data())
                        .data(answerRes.message()));
                } catch (IOException e) {
                    log.error("Failed to send stream event - chatRoomId: {}", chatRoomId, e);
                    emitter.completeWithError(e);
                }
            }))
            .doOnError(error -> {
                log.error("Stream processing error - chatRoomId: {}", chatRoomId, error);

                executor.execute(() -> {
                    sendErrorEvent(emitter, error, String.valueOf(chatRoomId));
                    emitter.complete();
                });
            })
            .doOnComplete(() -> {
                log.info("Stream completed successfully - chatRoomId: {}", chatRoomId);

                executor.execute(() -> {
                    try {
                        emitter.send(SseEmitter.event()
                            .id("CompleteNode")
                            .name("complete")
                            .data(Map.of(
                                "qnaNo", chatRoomId,
                                "data", "completed"
                            )));
                        emitter.complete();
                    } catch (IOException e) {
                        log.error("Failed to send completion event - chatRoomId: {}", chatRoomId,
                            e);
                        emitter.complete();
                    }
                });
            })
            .subscribe();

        emitter.onCompletion(() ->
            log.info("SSE connection completed - chatRoomId: {}", chatRoomId));
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
        return BaseResponseFactory.create(mariAgentService.patchQnaAssessment(chatRoomId, qnaNo, qnaAssessmentReq));
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
        return BaseResponseFactory.create(mariAgentService.postQnaRetry(chatRoomId, qnaNo, user, companyNo));
    }

}
