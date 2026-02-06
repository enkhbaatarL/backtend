package com.sk.domain.shared.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.shared.dto.response.common.CommonCodeDetailRes;
import com.sk.domain.shared.dto.response.common.UiComponentSettingRes;
import com.sk.domain.shared.dto.response.common.UserAccessPageRes;
import com.sk.domain.shared.entity.master.tam.AdminPermission;
import com.sk.domain.shared.entity.master.tcm.CommonCodeDetail;
import com.sk.domain.shared.entity.master.tcm.UiComponentSetting;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.UserAccessPageType;
import com.sk.domain.shared.enums.UserRoleCd;
import com.sk.domain.shared.repository.master.tam.AdminPermissionRepository;
import com.sk.domain.shared.repository.master.tcm.CommonCodeDetailRepository;
import com.sk.domain.shared.repository.master.tcm.CommonCodeRepository;
import com.sk.domain.shared.repository.master.tcm.UiComponentSettingRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.repository.master.tmd.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommonService {

    private final AdminPermissionRepository adminPermissionRepository;
    private final UserRepository userRepository;
    private final UiComponentSettingRepository uiComponentSettingRepository;
    private final CustomerCompanyRepository customerCompanyRepository;
    private final String ALL_ALLOW_IP_ADDRESS = "0.0.0.0"; // 20250620 기준 DB에 '0.0.0.0' IP 등록시 모든 IP 허용
    private final CommonCodeRepository commonCodeRepository;
    private final CommonCodeDetailRepository commonCodeDetailRepository;
    @Value("${server.domain}")
    private String miDefaultDomain;

    /**
     * 사용자 정보를 조회합니다.
     *
     * @param user
     * @return
     */
    public UserAccessPageRes getUserDetail(User user, HttpServletRequest request) {
        // User ID 조회
        List<User> userList = userRepository.findByUserLoginIdAndIsDeletedFalse(user.getUserLoginId());

        if (userList.isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("사용자 정보를 찾을 수 없습니다. userLoginId: %s", user.getUserLoginId()));
        }

        User miUser = userList.stream().filter(u -> u.getUserRoleCd().equals(UserRoleCd.USER)).findFirst().orElse(null);
        Boolean miUserCheck = miUser != null;

        // Admin or Operator 조회
        User adminUser = userList.stream().filter(u -> u.getUserRoleCd().equals(UserRoleCd.ADMIN) || u.getUserRoleCd().equals(UserRoleCd.OPERATOR))
                .findFirst().orElse(null);

        Boolean adminUserCheck = adminUser != null ? isAdminUser(adminUser, request) : false;

        UserAccessPageType userAccessPageType = (miUserCheck && adminUserCheck) ? UserAccessPageType.BOTH
                : (miUserCheck) ? UserAccessPageType.MI : (adminUserCheck) ? UserAccessPageType.ADMIN : UserAccessPageType.NONE;

        return UserAccessPageRes.of(user, userAccessPageType, miDefaultDomain);
    }

    /**
     * 관리자 또는 운영자의 경우 허용된 IP에서만 접근 가능
     *
     * @param user
     * @param request
     * @return
     */
    private Boolean isAdminUser(User user, HttpServletRequest request) {
        // 클라이언트 IP 주소 추출
        String clientIp = getClientIp(request);
        List<AdminPermission> existingPermissions = adminPermissionRepository.findByIdUserNoAndIsDeletedFalse(user.getUserNo());

        // IP 검증 로직 (예: 허용된 IP 목록에 clientIp가 포함되어 있는지 확인)
        return existingPermissions.stream()
                .anyMatch(permission -> permission.getId().getIpAddress().equals(clientIp)); // 허용 IP 필드가 있다고 가정
    }

    /**
     * 관리자 또는 운영자의 경우 허용된 IP에서만 접근 가능
     *
     * @param user
     * @param request
     * @return
     */
    public Boolean adminAccessCheck(User user, HttpServletRequest request) {
        // 관리자 또는 운영자의 경우 허용된 IP에서만 접근 가능
        if (!user.getUserRoleCd().equals(UserRoleCd.ADMIN) && !user.getUserRoleCd().equals(UserRoleCd.OPERATOR)) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("권한 없는 사용자 접근: %s", user.getUserLoginId()));
        }

        checkIp(user, request);
        return true;
    }

    /**
     * 관리자 또는 운영자의 경우 허용된 IP에서만 접근 가능
     *
     * @param user
     * @param request
     * @return
     */
    public void checkIp(User user, HttpServletRequest request) {
        // 클라이언트 IP 주소 추출
        String clientIp = getClientIp(request);

        List<AdminPermission> existingPermissions = adminPermissionRepository.findByIdUserNoAndIsDeletedFalse(user.getUserNo());

        // IP 검증 로직 (예: 허용된 IP 목록에 clientIp가 포함되어 있는지 확인)
        boolean isAllowedIp = existingPermissions.stream()
                .anyMatch(permission -> {
                    String ipAddress = permission.getId().getIpAddress();
                    return ipAddress.equals(clientIp) || ipAddress.equals(ALL_ALLOW_IP_ADDRESS);
                }); // 허용 IP 필드가 있다고 가정

        if (!isAllowedIp) {
            throw AppErrorException.of(ResultCode.Error.AUTH_IP_NOT_ALLOWED, String.format("권한 없는 IP 접근: %s", clientIp));
        }
    }

    /**
    * 클라이언트 IP 주소를 추출하는 메서드
    *
    * @param request
    * @return 클라이언트 IP 주소
    */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For 헤더에 여러 IP가 포함된 경우 첫 번째 IP만 사용
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 코드그룹ID로 공통코드상세 목록 조회
     *
     * @param codeGroupId 코드그룹ID
     * @return 공통코드상세 목록
     */
    public List<CommonCodeDetailRes> getCommonCodeDetails(String codeGroupId) {
        List<CommonCodeDetail> details = commonCodeDetailRepository.findValidCommonCodeDetails(codeGroupId);

        return details.stream()
                .map(detail -> new CommonCodeDetailRes(
                        detail.getId().getCodeId(),
                        detail.getCodeNm(),
                        detail.getCodeExpn()
                ))
                .toList();
    }

    /**
     * UI 설정 목록 조회
     *
     * @param companyNo 고객회사번호
     * @param screenCode 화면코드
     * @param componentTypeCode 컴포넌트타입코드
     * @param componentCode 컴포넌트코드
     * @return UI 설정 목록
     */
    public List<UiComponentSettingRes> getUiComponentSettings(Integer companyNo, String screenCode,
        String componentTypeCode, String componentCode) {
        customerCompanyRepository.findByCustomerCompanyNoAndIsDeletedFalse(companyNo)
            .orElseThrow(() -> AppErrorException.of(
                ResultCode.Error.RSC_NOT_FOUND,
                "customer company no: %d".formatted(companyNo)));

        List<UiComponentSetting> settings =
            uiComponentSettingRepository.findByCompanyAndOptionalFilters(
                companyNo, screenCode, componentTypeCode, componentCode);

        return settings.stream()
            .map(UiComponentSettingRes::from)
            .toList();
    }
}
