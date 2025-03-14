package com.sk.domain.shared.repository.master.tmd.custom;

import java.util.List;

import org.springframework.data.domain.Page;

import com.sk.domain.admin.dto.dataResult.UserWithCompany;
import com.sk.domain.admin.dto.response.UserWithCreatorRes;

public interface CustomUserRepository {

    List<UserWithCompany> findAllUserByInfoIds(List<String> userInfoReqs);

    Page<UserWithCreatorRes> findUsersByConditions(int page, int size, Integer customerCompanyNo, String departmentNm,
            String userName, String emailAddress);
}
