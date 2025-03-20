package com.sk.domain.shared.repository.master.tif.custom;

import org.springframework.data.domain.Page;

import com.sk.domain.admin.dto.response.UserInfoRes;

public interface CustomUserInfoRepository {

    Page<UserInfoRes> findUserInfosByConditions(int page, int size, String companyCd, String userNm,
            String emailAddress);

}
