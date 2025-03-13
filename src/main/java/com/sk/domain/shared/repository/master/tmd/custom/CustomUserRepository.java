package com.sk.domain.shared.repository.master.tmd.custom;

import java.util.List;

import com.sk.domain.admin.dto.dataResult.UserWithCompany;

public interface CustomUserRepository {

    List<UserWithCompany> findAllUserByInfoIds(List<String> userInfoReqs);
}
