package com.sk.domain.admin.dto.response;

import com.sk.domain.admin.dto.request.TestReq;
import com.sk.domain.shared.entity.master.tcm.ComponentInfo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record TestRes() {
    // getUserInfo에 대한 응답
    public record GetUserInfo(
            @Schema(description = "사용자 이름", example = "장민규") String name,
            @Schema(description = "이메일 주소", example = "mk.jang@twolinecode.com") String email
    ) {
        @Builder
        public GetUserInfo(String name, String email) {
            this.name = name;
            this.email = email;
        }

        public static GetUserInfo of(TestReq.getUserInfo testReq) {
            return GetUserInfo.builder()
                    .name(testReq.name())
                    .email(testReq.email())
                    .build();
        }
    }
}
