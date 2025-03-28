package com.sk.domain.admin.dto.response;

import com.sk.domain.admin.dto.request.TestReq;
import lombok.Builder;

public record TestRes(

        String name,
        String email

) {

    @Builder
    public TestRes(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public static TestRes of(TestReq.getUserInfo testReq) {
        return TestRes.builder()
                .name(testReq.name())
                .email(testReq.email())
                .build();
    }

}
