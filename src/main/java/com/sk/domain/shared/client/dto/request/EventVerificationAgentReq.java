package com.sk.domain.shared.client.dto.request;

import java.util.List;

/** actor는 클라이언트 입력이 아닌 인증된 로그인 사용자로 설정한다. */
public record EventVerificationAgentReq(List<Long> miEventIds, String actorUsrLognId) {}
