package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.annotation.UserResolve;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.request.agent.QnaAssessmentReq;
import com.sk.domain.mi.dto.request.agent.TalkReq;
import com.sk.domain.mi.dto.response.agent.MajorQuestionRes;
import com.sk.domain.mi.dto.response.agent.QnaRes;
import com.sk.domain.mi.dto.response.agent.TalkRes;
import com.sk.domain.mi.dto.response.agent.talk.AnswerRes;
import com.sk.domain.mi.service.MariAgentService;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "11. Mari Agent API", description = "AI Agent 채팅 서비스 Mari API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/mari-agent")
@RequiredArgsConstructor
public class MiMariAgentController {

    private final MariAgentService mariAgentService;

    @ApiOrder(1)
    @Parameters({
        @Parameter(name = "talkId", description = "챗봇대화 ID")
    })
    @Operation(summary = "주요 질의문 목록 조회 API", description = "분과 별 주요 질의문 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/major-questions")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<List<MajorQuestionRes>> getMajorQuestions(
        @RequestParam(value = "subcommitteeCd") String subcommitteeCd
    ) {
        return BaseResponseFactory.create(mariAgentService.getMajorQuestions(subcommitteeCd));
    }

    @ApiOrder(2)
    @Operation(summary = "대화 목록 조회 API", description = "로그인된 사용자의 Mari 에이전트 대화 목록을 조회합니다. (목록은 최신순으로 조회됩니다.)")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/talks")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<List<TalkRes>> getTalks(@UserResolve User user) {
        return BaseResponseFactory.create(mariAgentService.getTalks(user));
    }

    @ApiOrder(3)
    @Parameters({
        @Parameter(name = "talkId", description = "챗봇대화 ID"),
        @Parameter(name = "page", description = "페이지 번호 (기본값: 1)"),
        @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 3)")
    })
    @Operation(summary = "대화 QnA 목록 조회 API (pagination)", description = "로그인된 사용자의 Mari 에이전트 QnA 목록을 페이지네이션으로 조회합니다. (목록은 최신순으로 조회됩니다.)")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/talks/{talkId}")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<PaginationRes<QnaRes>> getTalkQnaList(
        @PathVariable(value = "talkId") String talkId,
        @RequestParam(value = "page", required = false, defaultValue = "1") int page,
        @RequestParam(value = "size", required = false, defaultValue = "3") int size,
        @UserResolve User user
    ) {
        return BaseResponseFactory.create(mariAgentService.getTalkQnaList(page, size, talkId, user));
    }

    @ApiOrder(4)
    @Operation(summary = "질의하기 API", description = "Mari 에이전트에게 질의합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/talk")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<QnaRes> postTalk(
        @RequestBody @Valid TalkReq.Insert talkReq,
        @UserResolve User user
    ) {
        return BaseResponseFactory.create(mariAgentService.postTalk(talkReq, user));
    }

    @ApiOrder(5)
    @Operation(summary = "질의 수정 API", description = "질의 내용을 수정합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PatchMapping("/talks/{talkId}")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<TalkRes> patchTalk(
        @PathVariable(value = "talkId") String talkId,
        @RequestBody @Valid TalkReq.Update talkReq,
        @UserResolve User user
    ) {
        return BaseResponseFactory.create(mariAgentService.patchTalk(talkId, talkReq, user));
    }

    @ApiOrder(6)
    @Operation(summary = "질의 삭제 API", description = "질의를 삭제합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @DeleteMapping("/talks/{talkId}")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<TalkRes> deleteTalk(
        @PathVariable(value = "talkId") String talkId,
        @UserResolve User user
    ) {
        return BaseResponseFactory.create(mariAgentService.deleteTalk(talkId, user));
    }

    @ApiOrder(7)
    @Parameters({
        @Parameter(name = "talkId", description = "챗봇대화 ID"),
        @Parameter(name = "qnaNo", description = "질의응답 번호")
    })
    @Operation(summary = "대화 QnA 평가 API", description = "Mari 에이전트의 응답을 평가합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PatchMapping("/talks/{talkId}/qna/{qnaNo}/assessment")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<?> patchQnaAssessment(
        @PathVariable(value = "talkId") String talkId,
        @PathVariable(value = "qnaNo") Integer qnaNo,
        @RequestBody @Valid QnaAssessmentReq qnaAssessmentReq
    ) {
        return BaseResponseFactory.create(mariAgentService.patchQnaAssessment(talkId, qnaNo, qnaAssessmentReq));
    }

    @ApiOrder(8)
    @Parameters({
        @Parameter(name = "talkId", description = "챗봇대화 ID"),
        @Parameter(name = "qnaNo", description = "질의응답 번호")
    })
    @Operation(summary = "QnA 답변 생성 재시도 API", description = "대화를 새로고침합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/talks/{talkId}/qna/{qnaNo}/retry")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<?> postQnaRetry(
        @PathVariable(value = "talkId") String talkId,
        @PathVariable(value = "qnaNo") Integer qnaNo,
        @UserResolve User user
    ) {
        return BaseResponseFactory.create(mariAgentService.postQnaRetry(talkId, qnaNo, user));
    }

}
