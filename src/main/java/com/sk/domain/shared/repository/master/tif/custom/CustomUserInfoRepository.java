package com.sk.domain.shared.repository.master.tif.custom;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;

import com.sk.domain.admin.dto.internal.UserInfoWithCompany;
import com.sk.domain.admin.dto.response.user.UserInfoRes;
import com.sk.domain.shared.enums.UserRoleCd;

public interface CustomUserInfoRepository {

    Page<UserInfoRes> findUserInfosByConditions(int page, int size, List<UserRoleCd> userRoleCds, String companyCd, String userNm,
            String emailAddress);

    List<UserInfoWithCompany> findAllUserByInfoIds(List<String> imCds);

    Optional<UserInfoWithCompany> findUserByInfoId(String imCd);
}
