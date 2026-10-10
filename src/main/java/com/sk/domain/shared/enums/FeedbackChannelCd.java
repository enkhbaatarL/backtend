package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * [MCP Feedback] 사용자 피드백 채널 코드 (tcm_usrfedbl.fedb_chnl_cd). DB 기본값은 SCREEN 이다.
 */
@Getter
@RequiredArgsConstructor
public enum FeedbackChannelCd {

    SCREEN("SCREEN", "화면"),
    MCP("MCP", "MCP");

    private final String code;
    private final String description;

}
