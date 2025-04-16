package com.sk.domain.shared.repository.master.tif.custom;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;

import com.sk.domain.admin.dto.internal.UserInfoWithCompany;
import com.sk.domain.admin.dto.response.user.UserInfoRes;
import com.sk.domain.shared.enums.UserRoleCd;

public interface CustomUserInfoRepository {

    /**
     * 인터페이스 테이블에서 사용자 정보를 조회합니다.
     * 
     * @param page              조회할 페이지 번호 (min 1)
     * @param size              조회할 요소 사이즈 (min 1)
     * @param userRoleCd        사용자역할코드
     * @param companyCd         회사코드
     * @param userNm            사용자명
     * @param emailAddress      이메일주소
     * @return Page<UserInfoRes>
     */
    Page<UserInfoRes> findUserInfosByConditions(int page, int size, UserRoleCd userRoleCd, String companyCd, String userNm,
            String emailAddress);

    /**
     * 인터페이스 테이블에서 사용자 정보를 조회합니다.
     * 
     * @param imCds 사용자 ID 목록
     * @param userRoleCd 사용자 역할 코드
     * @return List<UserInfoWithCompany>
     */
    List<UserInfoWithCompany> findAllUserByInfoIds(List<String> imCds, UserRoleCd userRoleCd);

    /**
     * 인터페이스 테이블에서 사용자 정보를 조회합니다.
     * 
     * @param imCd 사용자 ID
     * @param userRoleCd 사용자 역할 코드
     * @return Optional<UserInfoWithCompany>
     */
    Optional<UserInfoWithCompany> findUserByInfoId(String imCd, UserRoleCd userRoleCd);
}
