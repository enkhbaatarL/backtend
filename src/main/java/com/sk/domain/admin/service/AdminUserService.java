package com.sk.domain.admin.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.internal.UserInfoWithCompany;
import com.sk.domain.admin.dto.request.user.AdminReq;
import com.sk.domain.admin.dto.request.user.UserReq;
import com.sk.domain.admin.dto.response.user.AdminRes;
import com.sk.domain.admin.dto.response.user.UserDetailRes;
import com.sk.domain.admin.dto.response.user.UserInfoRes;
import com.sk.domain.admin.dto.response.user.UserRes;
import com.sk.domain.shared.entity.compositeKey.tmd.UserHistoryId;
import com.sk.domain.shared.entity.master.tam.AdminPermission;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.entity.master.tmd.UserHistory;
import com.sk.domain.shared.enums.CompanyType;
import com.sk.domain.shared.enums.UserRoleCd;
import com.sk.domain.shared.repository.master.tam.AdminPermissionRepository;
import com.sk.domain.shared.repository.master.tif.UserInfoRepository;
import com.sk.domain.shared.repository.master.tmd.UserHistoryRepository;
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
    private final AdminPermissionRepository adminPermissionRepository;
    private final UserHistoryRepository userHistoryRepository;

    /**
     * 사용자 목록을 원장 테이블에서 페이지네이션 처리하여 조회합니다.
     * 
     * @param page         조회할 페이지 번호 (min 1)
     * @param size         조회할 요소 사이즈 (min 1)
     * @param companyCd    회사코드
     * @param userNm       사용자명
     * @param emailAddress 이메일주소
     * @return PaginationRes<UserInfoRes>
     */
    public PaginationRes<UserInfoRes> getUserInfos(int page, int size, String companyCd, String userNm,
            String emailAddress) {
        return PaginationRes
                .toRes(userInfoRepository.findUserInfosByConditions(page, size, UserRoleCd.USER, companyCd, userNm,
                        emailAddress));
    }

    /**
    * 사용자 목록을 원장 테이블에서 페이지네이션 처리하여 조회합니다.
    * 
    * @param page         조회할 페이지 번호 (min 1)
    * @param size         조회할 요소 사이즈 (min 1)
    * @param companyCd    회사코드
    * @param userNm       사용자명
    * @param emailAddress 이메일주소
    * @return PaginationRes<UserInfoRes>
    */
    public PaginationRes<UserInfoRes> getAdminUserInfos(int page, int size, CompanyType companyType, String companyCd, String userNm,
            String emailAddress) {

        // 고객사는 운영자, 관리사는 관리자
        return PaginationRes
                .toRes(userInfoRepository.findUserInfosByConditions(page, size,
                        companyType == CompanyType.CUSTOMER ? UserRoleCd.OPERATOR : UserRoleCd.ADMIN, companyCd, userNm,
                        emailAddress));
    }

    /**
     * 사용자 정보를 MI 서비스에 등록합니다.
     * 
     * @param userInfoReqs 원장 테이블에서 사용자 ID 목록
     * @return List<UserRes>
     */
    @Transactional
    public List<UserRes> saveUserInfo(UserReq.Insert userInfoReqs) {
        List<UserInfoWithCompany> userInfoWithCompanies = userInfoRepository.findAllUserByInfoIds(userInfoReqs.imCd(), UserRoleCd.USER);
        if (userInfoWithCompanies.size() != userInfoReqs.imCd().size()) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "IM코드 목록에 데이터가 존재하지 않습니다.");
        }

        List<User> existingUsers = userRepository.findByUserLoginIdsAndRoleAndNotDeleted(
                userInfoWithCompanies.stream().map(
                        userInfoWithCompany -> {
                            if (userInfoWithCompany.customerCompany() == null) {
                                throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                                        String.format("고객사로 등록되지 않은 회사의 사용자입니다: %s", userInfoWithCompany.userLoginId()));
                            }
                            return userInfoWithCompany.userLoginId();
                        }).collect(Collectors.toList()),
                UserRoleCd.USER);

        if (!existingUsers.isEmpty()) {
            String duplicatedUserNames = existingUsers.stream()
                    .map(User::getUserNm)
                    .collect(Collectors.joining(", "));
            throw AppErrorException.of(
                    ResultCode.Error.RSC_ALREADY_EXISTS,
                    "이미 등록된 유저가 있습니다: " + duplicatedUserNames);
        }

        List<User> users = userInfoWithCompanies.stream()
                .map(userInfoWithCompany -> User.of(userInfoWithCompany, UserRoleCd.USER))
                .collect(Collectors.toList());

        LocalDateTime now = LocalDateTime.now();
        List<UserHistory> histories = users.stream()
                .map(user -> UserHistory.builder()
                        .id(new UserHistoryId(user.getUserNo(), now))
                        .isOtherCompanyServiceAccessPossible(user.getIsOtherServiceAccessPossible())
                        .user(user)
                        .build())
                .toList();

        List<User> savedUsers = userRepository.saveAll(users);

        userHistoryRepository.saveAll(histories);

        return savedUsers.stream()
                .map(UserRes::ofSimple)
                .collect(Collectors.toList());
    }

    /**
     * 사용자 목록을 조회합니다.
     * 
     * @param page              조회할 페이지 번호 (min 1)
     * @param size              조회할 요소 사이즈 (min 1)
     * @param userRoleCd        사용자역할코드
     * @param customerCompanyNo 고객회사번호
     * @param departmentNm      부서명
     * @param userNm            사용자명
     * @param emailAddress      이메일주소
     * @return PaginationRes<UserDetailRes>
     */
    public PaginationRes<UserDetailRes> getUsers(int page, int size, Integer customerCompanyNo, String departmentNm, String userNm,
            String emailAddress) {

        return PaginationRes.toRes(userRepository.findUsersByConditions(page, size, List.of(UserRoleCd.USER), customerCompanyNo,
                departmentNm, userNm, emailAddress));
    }

    /** 로그인 ID 조회 결과의 첫 계정을 선택하고 기존 번호 상세 조회를 재사용합니다. */
    public UserDetailRes getUserByLoginId(String userLoginId) {
        User selected = userRepository.findByUserLoginIdAndIsDeletedFalse(userLoginId).stream()
                .findFirst()
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND));
        return getUser(selected.getUserNo());
    }

    /**
     * 사용자 상세 정보를 조회합니다.
     * 
     * @param userNo 사용자번호
     * @return UserDetailRes
     */
    public UserDetailRes getUser(Integer userNo) {
        return userRepository.findUserByConditions(userNo)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                        "사용자 정보를 찾을 수 없습니다."));
    }

    /**
     * 사용자 정보를 삭제합니다.
     * 
     * @param userNo 사용자번호
     * @return UserRes
     */
    @Transactional
    public UserRes deleteUser(Integer userNo) {
        User user = userRepository.findByUserNoAndUserRoleCdList(userNo, List.of(UserRoleCd.USER))
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                        String.format("사용자 정보를 찾을 수 없습니다. userNo: %s", userNo)));

        user.userDelete();

        UserHistory userHistory = UserHistory.builder()
                .id(new UserHistoryId(user.getUserNo(), LocalDateTime.now()))
                .isOtherCompanyServiceAccessPossible(user.getIsOtherServiceAccessPossible())
                .user(user)
                .build();
        userHistoryRepository.save(userHistory);

        return UserRes.ofSimple(user);
    }

    /**
     * 어드민 관리자 정보를 등록합니다.
     * 
     * @param adminReq 어드민 관리자 정보
     * @return UserRes
     */
    @Transactional
    public UserRes saveAdminUser(AdminReq.Insert adminReq) {
        UserInfoWithCompany userInfoWithCompany = userInfoRepository.findUserByInfoId(adminReq.imCd(), adminReq.userRoleCd())
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                        String.format("The data does not exist in the IM code list. imCd: %s", adminReq.imCd())));

        if (userInfoWithCompany.customerCompany() == null) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                    String.format("고객사로 등록되지 않은 회사의 사용자입니다: %s", userInfoWithCompany.userLoginId()));
        }

        userRepository.findByUserLoginIdAndUserRoleCdAndIsDeletedFalse(userInfoWithCompany.userLoginId(), adminReq.userRoleCd())
                .ifPresent(u -> {
                    throw AppErrorException.of(ResultCode.Error.RSC_ALREADY_EXISTS,
                            String.format("이미 등록된 사용자입니다: %s", u.getUserNm()));
                });

        User user = User.of(userInfoWithCompany, adminReq.userRoleCd());
        User savedUser = userRepository.save(user);

        UserHistory userHistory = UserHistory.builder()
                .id(new UserHistoryId(savedUser.getUserNo(), LocalDateTime.now()))
                .isOtherCompanyServiceAccessPossible(savedUser.getIsOtherServiceAccessPossible())
                .user(savedUser)
                .build();
        userHistoryRepository.save(userHistory);

        List<AdminPermission> existingPermissions = adminPermissionRepository
                .findByIdUserNoAndIsDeletedFalse(savedUser.getUserNo());
        Set<String> existingIpSet = existingPermissions.stream()
                .map(permission -> permission.getId().getIpAddress())
                .collect(Collectors.toSet());

        List<String> newIpAddresses = Optional.ofNullable(adminReq.accessAllowedIpAddress())
                .orElse(Collections.emptyList())
                .stream()
                .filter(ip -> !existingIpSet.contains(ip))
                .toList();

        newIpAddresses.forEach(ipAddress -> {
            adminPermissionRepository.save(AdminPermission.of(savedUser, ipAddress));
        });

        return UserRes.ofSimple(savedUser);
    }

    /**
     * 사용자 목록을 조회합니다.
     * 운영자와 관리자는 한 화면에서 조회
     * 
     * @param page              조회할 페이지 번호 (min 1)
     * @param size              조회할 요소 사이즈 (min 1)
     * @param userRoleCd        사용자역할코드
     * @param customerCompanyNo 고객회사번호
     * @param departmentNm      부서명
     * @param userNm            사용자명
     * @param emailAddress      이메일주소
     * @return PaginationRes<UserDetailRes>
     */
    public PaginationRes<UserDetailRes> getAdminUsers(int page, int size, UserRoleCd userRoleCd, Integer customerCompanyNo, String departmentNm,
            String userNm,
            String emailAddress) {

        if (userRoleCd != null && userRoleCd == UserRoleCd.USER) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID, String.format("UserRoleCd: %s는 사용할 수 없습니다.", UserRoleCd.USER));
        }

        // 사용자 역할 코드가 없으면 모든 운영자 / 관리자 사용자 조회
        List<UserRoleCd> userRoleCdsCondition = null;
        if (userRoleCd == null) {
            userRoleCdsCondition = List.of(UserRoleCd.ADMIN, UserRoleCd.OPERATOR);
        } else {
            userRoleCdsCondition = List.of(userRoleCd);
        }

        return PaginationRes.toRes(userRepository.findUsersByConditions(page, size, userRoleCdsCondition, customerCompanyNo,
                departmentNm, userNm, emailAddress));
    }

    /**
     * 어드민 관리자 상세 정보를 조회합니다.
     * 
     * @param userNo 사용자번호
     * @return AdminRes
     */
    public AdminRes getAdminUser(Integer userNo) {
        User user = userRepository.findByUserNoAndIsDeletedFalse(userNo)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                        "사용자 정보를 찾을 수 없습니다."));

        List<AdminPermission> adminPermissions = adminPermissionRepository.findByIdUserNoAndIsDeletedFalse(userNo);

        return AdminRes.of(user, adminPermissions);
    }

    /**
     * 어드민 관리자 정보를 수정합니다.
     * 현재 로직은 한 계정으로 관리자(ADMIN) / 운영자(OPERATOR) 두 가지 역할이 Switching이 가능하다는 전제에 구현되어 있습니다.
     * 
     * @param userNo   사용자번호
     * @param adminReq 어드민 관리자 정보
     * @return UserRes
     */
    @Transactional
    public UserRes patchAdminUser(Integer userNo, AdminReq.Update adminReq) {
        User user = userRepository.findByUserNoAndIsDeletedFalse(userNo)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                        "사용자 정보를 찾을 수 없습니다."));
        user.update(adminReq);

        UserHistory userHistory = UserHistory.builder()
                .id(new UserHistoryId(user.getUserNo(), LocalDateTime.now()))
                .isOtherCompanyServiceAccessPossible(
                        adminReq.isOtherServiceAccessPossible() != null ? adminReq.isOtherServiceAccessPossible()
                                : user.getIsOtherServiceAccessPossible())
                .user(user)
                .build();
        userHistoryRepository.save(userHistory);

        // 1. DB에서 기존 IP 데이터 조회
        List<AdminPermission> existingPermissions = adminPermissionRepository
                .findByIdUserNoAndIsDeletedFalse(userNo);

        // 2. DTO에서 받은 IP 목록 (null 체크 포함)
        List<String> requestedIpAddresses = Optional.ofNullable(adminReq.accessAllowedIpAddress())
                .orElse(Collections.emptyList());

        // 3. DB IP 목록 추출
        Set<String> existingIpSet = existingPermissions.stream()
                .map(permission -> permission.getId().getIpAddress())
                .collect(Collectors.toSet());

        // 4. DTO IP 목록 Set으로 변환
        Set<String> requestedIpSet = new HashSet<>(requestedIpAddresses);

        // 5. 삭제할 IP (DB에 있는데 DTO에 없음)
        List<AdminPermission> toDelete = existingPermissions.stream()
                .filter(permission -> !requestedIpSet.contains(permission.getId().getIpAddress()))
                .toList();
        toDelete.forEach(AdminPermission::softDelete);

        List<String> toInsert = requestedIpAddresses.stream()
                .filter(ip -> !existingIpSet.contains(ip))
                .toList();

        List<AdminPermission> newPermissions = toInsert.stream()
                .map(ip -> AdminPermission.of(user, ip))
                .toList();
        adminPermissionRepository.saveAll(newPermissions);

        return UserRes.ofSimple(user);
    }

    /**
     * 어드민 관리자 정보를 삭제합니다.
     * 현재 로직은 한 계정으로 관리자(ADMIN) / 운영자(OPERATOR) 두 가지 역할을 가질 수 없다는 전제에 구현되어 있습니다.
     * 
     * @param userNo 사용자번호
     * @return UserRes
     */
    @Transactional
    public UserRes deleteAdminUser(Integer userNo) {
        User user = userRepository.findByUserNoAndUserRoleCdList(userNo, List.of(UserRoleCd.ADMIN, UserRoleCd.OPERATOR))
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                        String.format("사용자 정보를 찾을 수 없습니다. userNo: %s", userNo)));

        user.userDelete();

        adminPermissionRepository.findByIdUserNoAndIsDeletedFalse(userNo).forEach(AdminPermission::softDelete);

        UserHistory userHistory = UserHistory.builder()
                .id(new UserHistoryId(user.getUserNo(), LocalDateTime.now()))
                .isOtherCompanyServiceAccessPossible(user.getIsOtherServiceAccessPossible())
                .user(user)
                .build();
        userHistoryRepository.save(userHistory);

        return UserRes.ofSimple(user);
    }
}
