package com.sk.domain.admin.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.dataResult.UserWithCompany;
import com.sk.domain.admin.dto.request.UserReq;
import com.sk.domain.admin.dto.response.UserInfoRes;
import com.sk.domain.admin.dto.response.UserRes;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.repository.master.tif.UserInfoRepository;
import com.sk.domain.shared.repository.master.tmd.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminUserService {

    private final UserInfoRepository userInfoRepository;
    private final UserRepository userRepository;

    /**
     * 사용자 목록을 원장 테이블에서 페이지네이션 처리하여 조회합니다.
     * 
     * @param page         조회할 페이지 번호 (min 1)
     * @param size         조회할 요소 사이즈 (min 1)
     * @param companyCode  회사코드
     * @param userName     사용자명
     * @param emailAddress 이메일주소
     * @return PaginationRes<UserInfoRes>
     */
    public PaginationRes<UserInfoRes> getUserInfos(int page, int size, String companyCode, String userName,
            String emailAddress) {
        return PaginationRes.toRes(userInfoRepository.findUserInfosByConditions(page, size, companyCode, userName,
                emailAddress));
    }

    /**
     * 사용자 정보를 MI 서비스에 등록합니다.
     * 
     * @param userInfoReq 원장 테이블에서 사용자 ID 목록
     * @return UserInfoRes
     */
    public List<UserRes> postUserInfo(List<UserReq.Insert> userInfoReqs) {
        List<String> imCodes = userInfoReqs.stream()
                .map(UserReq.Insert::imCode)
                .collect(Collectors.toList());

        List<UserWithCompany> userWithCompanies = userRepository.findAllUserByInfoIds(imCodes);
        if (userWithCompanies.size() != imCodes.size()) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "IM코드 목록에 데이터가 존재하지 않습니다.");
        }

        List<User> users = userWithCompanies.stream()
                .map(User::of)
                .collect(Collectors.toList());

        return userRepository.saveAll(users).stream()
                .map(UserRes::of)
                .collect(Collectors.toList());
    }
}