package com.sk.domain.admin.service;

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
import com.sk.domain.admin.dto.dataResult.UserWithCompany;
import com.sk.domain.admin.dto.request.AdminReq;
import com.sk.domain.admin.dto.request.UserReq;
import com.sk.domain.admin.dto.response.AdminRes;
import com.sk.domain.admin.dto.response.UserInfoRes;
import com.sk.domain.admin.dto.response.UserRes;
import com.sk.domain.admin.dto.response.UserWithCompanyRes;
import com.sk.domain.admin.dto.response.UserWithCreatorRes;
import com.sk.domain.shared.entity.master.tam.AdminPermission;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.UserRoleCd;
import com.sk.domain.shared.repository.master.tam.AdminPermissionRepository;
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
	private final AdminPermissionRepository adminPermissionRepository;

	/**
	 * 사용자 목록을 원장 테이블에서 페이지네이션 처리하여 조회합니다.
	 * 
	 * @param page         조회할 페이지 번호 (min 1)
	 * @param size         조회할 요소 사이즈 (min 1)
	 * @param companyCd  회사코드
	 * @param userNm     사용자명
	 * @param emailAddress 이메일주소
	 * @return PaginationRes<UserInfoRes>
	 */
	public PaginationRes<UserInfoRes> getUserInfos(int page, int size, UserRoleCd userRoleCd, String companyCd, String userNm,
			String emailAddress) {
		return PaginationRes
				.toRes(userInfoRepository.findUserInfosByConditions(page, size, userRoleCd, companyCd, userNm,
						emailAddress));
	}

	/**
	 * 사용자 정보를 MI 서비스에 등록합니다.
	 * 
	 * @param List<UserReq.Insert> 원장 테이블에서 사용자 ID 목록
	 * @return List<UserRes>
	 */
	//TODO: 호영:사용자 이력 로그 추가
	@Transactional
	public List<UserRes> postUserInfo(List<UserReq.Insert> userInfoReqs) {
		List<String> imCds = userInfoReqs.stream()
				.map(UserReq.Insert::imCd)
				.collect(Collectors.toList());

		List<UserWithCompany> userWithCompanies = userRepository.findAllUserByInfoIds(imCds);
		if (userWithCompanies.size() != imCds.size()) {
			throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "IM코드 목록에 데이터가 존재하지 않습니다.");
		}

		List<User> existingUsers = userRepository.findByUserLoginIdsAndRoleAndNotDeleted(
				userWithCompanies.stream().map(UserWithCompany::userLoginId).collect(Collectors.toList()),
				UserRoleCd.USER);

		if (!existingUsers.isEmpty()) {
			String duplicatedUserNames = existingUsers.stream()
					.map(User::getUserNm)
					.collect(Collectors.joining(", "));
			throw AppErrorException.of(
					ResultCode.Error.RSC_ALREADY_EXISTS,
					"이미 등록된 유저가 있습니다: " + duplicatedUserNames);
		}

		List<User> users = userWithCompanies.stream()
				.map(userWithCompany -> User.of(userWithCompany, UserRoleCd.USER))
				.collect(Collectors.toList());

		return userRepository.saveAll(users).stream()
				.map(UserRes::of)
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
	 * @return PaginationRes<UserWithCreatorRes>
	 */
	public PaginationRes<UserWithCreatorRes> getUsers(int page, int size, UserRoleCd userRoleCd,
			Integer customerCompanyNo,
			String departmentNm, String userNm, String emailAddress) {

		return PaginationRes.toRes(userRepository.findUsersByConditions(page, size, userRoleCd, customerCompanyNo,
				departmentNm, userNm, emailAddress));
	}

	/**
	 * 사용자 상세 정보를 조회합니다.
	 * 
	 * @param userNo 사용자번호
	 * @return UserWithCompanyRes
	 */
	public UserWithCompanyRes getUser(Integer userNo) {
		User user = userRepository.findByUserNoAndIsDeletedFalse(userNo)
				.orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
						"사용자 정보를 찾을 수 없습니다."));
		return UserWithCompanyRes.of(user);
	}

	/**
	 * 사용자 정보를 삭제합니다.
	 * 
	 * @param userNo 사용자번호
	 * @return UserRes
	 */
	@Transactional
	public UserRes deleteUser(Integer userNo) {
		User user = userRepository.findByUserNoAndIsDeletedFalse(userNo)
				.orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
						"사용자 정보를 찾을 수 없습니다."));
		user.softDelete();
		return UserRes.of(user);
	}

	/**
	 * 어드민 관리자 정보를 등록합니다.
	 * 
	 * @param adminReq 어드민 관리자 정보
	 * @return UserRes
	 */
	//TODO: 호영:사용자 이력 로그 추가
	@Transactional
	public UserRes postAdminUser(AdminReq.Insert adminReq) {
		UserWithCompany userWithCompany = userRepository.findUserByInfoId(adminReq.imCd())
				.orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "IM코드 목록에 데이터가 존재하지 않습니다."));

		userRepository.findByUserLoginIdAndUserRoleCdAndIsDeletedFalse(userWithCompany.userLoginId(), UserRoleCd.ADMIN)
				.ifPresent(u -> {
					throw AppErrorException.of(ResultCode.Error.RSC_ALREADY_EXISTS, "이미 등록된 관리자입니다: " + u.getUserNm());
				});

		User user = User.of(userWithCompany, UserRoleCd.ADMIN);
		User savedUser = userRepository.save(user);

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

		return UserRes.of(savedUser);
	}

	/**
	 * 어드민 관리자 상세 정보를 조회합니다.
	 * 
	 * @param userNo 사용자번호
	 * @return UserRes
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

		return UserRes.of(user);
	}

	/**
	 * 어드민 관리자 정보를 삭제합니다. (관리자 -> 유저/관리자 삭제에 로직(권한 등) 차이가 있을 경우 분리하여 사용)
	 * 
	 * @param userNo 사용자번호
	 * @return UserRes
	 */
	@Transactional
	public UserRes deleteAdminUser(Integer userNo) {
		User user = userRepository.findByUserNoAndIsDeletedFalse(userNo)
				.orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
						"사용자 정보를 찾을 수 없습니다."));
		user.softDelete();
		return UserRes.of(user);
	}
}
