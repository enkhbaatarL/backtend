package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.annotation.UserResolve;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.request.agent.ChatRoomReq;
import com.sk.domain.mi.dto.request.agent.QnaAssessmentReq;
import com.sk.domain.mi.dto.request.agent.QnaReq;
import com.sk.domain.mi.dto.response.agent.ChatRoomRes;
import com.sk.domain.mi.dto.response.agent.MajorQuestionRes;
import com.sk.domain.mi.dto.response.agent.QnaRes;
import com.sk.domain.mi.service.MariAgentService;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "11. Mari Agent API", description = "AI Agent 채팅 서비스 Mari API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/mari-agent")
@RequiredArgsConstructor
public class MiMariAgentController {

    private final MariAgentService mariAgentService;

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
    @Parameters({
        @Parameter(name = "chatRoomId", description = "채팅룸 ID"),
    })
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
    @Parameters({
        @Parameter(name = "chatRoomId", description = "채팅룸 ID"),
    })
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
    @Parameters({
        @Parameter(name = "chatRoomId", description = "채팅룸 ID (UUID)"),
        @Parameter(name = "page", description = "페이지 번호 (기본값: 1)"),
        @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 3)")
    })
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
    @Parameters({
        @Parameter(name = "chatRoomId", description = "채팅룸 ID (UUID)"),
    })
    @Operation(summary = "질의 API", description = "Mari 에이전트에게 질의합니다. <br>새로운 채팅룸에서 대화를 시작하려면 `chatRoomId`를 UUID로 생성해서 전달합니다. 존재하지 않는 `chatRoomId` 전달 시, 해당 ID로 채팅룸을 생성하고 첫 번째 QnA로 등록합니다. <br>기존 채팅룸에서 대화를 이어가려면 기존의 `chatRoomId`를 전달합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/chat-rooms/{chatRoomId}/qna")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<QnaRes> postChatRoomQna(
        @PathVariable(value = "chatRoomId") UUID chatRoomId,
        @RequestBody @Valid QnaReq.Insert qnaReq,
        @UserResolve User user
    ) {
        return BaseResponseFactory.create(mariAgentService.postChatRoomQna(chatRoomId, qnaReq, user));
    }

    @ApiOrder(7)
    @Parameters({
        @Parameter(name = "chatRoomId", description = "채팅룸 ID"),
        @Parameter(name = "qnaNo", description = "질의응답 번호")
    })
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

    @ApiOrder(8)
    @Parameters({
        @Parameter(name = "chatRoomId", description = "채팅룸 ID"),
        @Parameter(name = "qnaNo", description = "질의응답 번호")
    })
    @Operation(summary = "채팅룸 QnA 답변 생성 재시도 API", description = "Mari 에이전트의 응답을 재생성합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/chat-rooms/{chatRoomId}/qna/{qnaNo}/retry")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<?> postQnaRetry(
        @PathVariable(value = "chatRoomId") UUID chatRoomId,
        @PathVariable(value = "qnaNo") Integer qnaNo,
        @UserResolve User user
    ) {
        return BaseResponseFactory.create(mariAgentService.postQnaRetry(chatRoomId, qnaNo, user));
    }

}
