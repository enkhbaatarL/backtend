package com.sk.domain.shared.repository.master.tmd.custom;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;

import com.sk.domain.admin.dto.dataResult.UserWithCompany;
import com.sk.domain.admin.dto.response.UserWithCreatorRes;
import com.sk.domain.shared.entity.master.tif.UserInfo;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.UserRoleCd;

public interface CustomUserRepository {

    List<UserWithCompany> findAllUserByInfoIds(List<String> imCds);

    Optional<UserWithCompany> findUserByInfoId(String imCd);

    Page<UserWithCreatorRes> findUsersByConditions(int page, int size, UserRoleCd userRoleCd, Integer customerCompanyNo,
            String departmentNm, String userNm, String emailAddress);

    List<User> findByUserLoginIdsAndRoleAndNotDeleted(List<String> userLoginIds, UserRoleCd role);
    
    List<User> findByUserInfosAndIsDeletedFalse(List<UserInfo> userInfos);
}
