package com.sk.domain.shared.repository.master.tmd.custom;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;

import com.sk.domain.admin.dto.response.user.UserDetailRes;
import com.sk.domain.shared.entity.master.tif.UserInfo;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.UserRoleCd;

public interface CustomUserRepository {
    /**
     * 사용자 목록을 조회합니다.
     * 
     * @param page              조회할 페이지 번호 (min 1)
     * @param size              조회할 요소 사이즈 (min 1)
     * @param userRoleCds       사용자 역할 코드 목록
     * @param customerCompanyNo 고객회사번호
     * @param departmentNm      부서명
     * @param userNm            사용자명
     * @param emailAddress      이메일주소
     * @return Page<UserDetailRes>
     */
    Page<UserDetailRes> findUsersByConditions(int page, int size, List<UserRoleCd> userRoleCds, Integer customerCompanyNo,
            String departmentNm, String userNm, String emailAddress);

    /**
     * 사용자 목록을 조회합니다.
     * 
     * @param userNo 사용자 번호
     * @return Optional<UserDetailRes>
     */
    Optional<UserDetailRes> findUserByConditions(int userNo);

    /**
     * 사용자 목록을 조회합니다.
     * 
     * @param userLoginIds 사용자 로그인 ID 목록
     * @param role         사용자 역할 코드
     * @return List<User>
     */
    List<User> findByUserLoginIdsAndRoleAndNotDeleted(List<String> userLoginIds, UserRoleCd role);

    /**
     * 사용자 목록을 조회합니다.
     * 
     * @param userInfos 사용자 정보 목록
     * @return List<User>
     */
    List<User> findByUserInfosAndIsDeletedFalse(List<UserInfo> userInfos);

    /**
     * 사용자 목록을 조회합니다.
     * 
     * @param userNo 사용자 번호
     * @param userRoleCds 사용자 역할 코드 목록
     * @return Optional<User>
     */
    Optional<User> findByUserNoAndUserRoleCdList(Integer userNo, List<UserRoleCd> userRoleCds);
}
