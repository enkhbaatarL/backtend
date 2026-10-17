package com.sk.domain.mi.dto.response.agent;

import com.sk.domain.mi.dto.response.agent.talk.AnswerRes;
import reactor.core.publisher.Flux;

/**
 * Stream 응답과 qnaNo를 함께 전달하기 위한 Wrapper 클래스
 */
public record StreamResponseWrapper(Integer qnaNo, Flux<AnswerRes> stream) {

  public static StreamResponseWrapper of(Integer qnaNo, Flux<AnswerRes> stream) {
    return new StreamResponseWrapper(qnaNo, stream);
  }
}

