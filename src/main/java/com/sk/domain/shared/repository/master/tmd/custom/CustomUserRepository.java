package com.sk.domain.shared.repository.master.tmd.custom;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;

import com.sk.domain.admin.dto.response.UserDetailRes;
import com.sk.domain.shared.entity.master.tif.UserInfo;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.UserRoleCd;

public interface CustomUserRepository {
    Page<UserDetailRes> findUsersByConditions(int page, int size, List<UserRoleCd> userRoleCds, Integer customerCompanyNo,
            String departmentNm, String userNm, String emailAddress);

    List<User> findByUserLoginIdsAndRoleAndNotDeleted(List<String> userLoginIds, UserRoleCd role);
    
    List<User> findByUserInfosAndIsDeletedFalse(List<UserInfo> userInfos);

    Optional<User> findByUserNoAndUserRoleCdList(Integer userNo, List<UserRoleCd> userRoleCds);
}
