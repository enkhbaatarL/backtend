package com.sk.domain.mi.dto.response.agent;

import com.sk.domain.mi.dto.response.agent.talk.AnswerRes;
import lombok.Getter;
import reactor.core.publisher.Flux;

/**
 * Stream 응답과 qnaNo를 함께 전달하기 위한 Wrapper 클래스
 */
@Getter
public class StreamResponseWrapper {
    private final Integer qnaNo;
    private final Flux<AnswerRes> stream;

    public StreamResponseWrapper(Integer qnaNo, Flux<AnswerRes> stream) {
        this.qnaNo = qnaNo;
        this.stream = stream;
    }

    public static StreamResponseWrapper of(Integer qnaNo, Flux<AnswerRes> stream) {
        return new StreamResponseWrapper(qnaNo, stream);
    }
}

