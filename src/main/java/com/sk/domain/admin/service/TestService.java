package com.sk.domain.admin.service;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.sk.common.config.database.dataSource.DataSourceContextHolder;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.JwtUtil;
import com.sk.domain.admin.dto.request.TestLoginReq;
import com.sk.domain.admin.dto.request.TestReq;
import com.sk.domain.admin.dto.request.TestVerifyTokenReq;
import com.sk.domain.admin.dto.response.TestLoginRes;
import com.sk.domain.admin.dto.response.TestRes;
import com.sk.domain.shared.entity.master.tlm.EmailAccessLog;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.UserRoleCd;
import com.sk.domain.shared.mapper.hynix.tlm.HynixEmailAccessLogMapper;
import com.sk.domain.shared.mapper.sktelecom.tlm.SkTelecomEmailAccessLogMapper;
import com.sk.domain.shared.repository.master.tam.AdminPermissionRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.repository.master.tmd.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestService {

    private final JwtUtil jwtUtil;
    private final SkTelecomEmailAccessLogMapper skTelecomEmailAccessLogMapper;
    private final HynixEmailAccessLogMapper hynixEmailAccessLogMapper;
    private final UserRepository userRepository;
    private final CustomerCompanyRepository customerCompanyRepository;
    private final AdminPermissionRepository adminPermissionRepository;

    public TestRes getUserInfo(TestReq testReq) {
        return TestRes.of(testReq);
    }

    /**
     * 사용자 로그인
     * 
     * @param testLoginReq
     * @return
     */
    @Transactional
    public TestLoginRes userLogin(TestLoginReq testLoginReq) {
        User user = userRepository.findByUserLoginIdAndUserRoleCdAndIsDeletedFalse(testLoginReq.loginUserId(),
                UserRoleCd.USER)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "사용자 정보가 없습니다."));

        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        return TestLoginRes.of(accessToken, refreshToken);
    }

    /**
     * 관리자 로그인
     * 
     * @param testLoginReq
     * @return
     */
    @Transactional
    public TestLoginRes adminLogin(TestLoginReq testLoginReq) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        String clientIp = request.getRemoteAddr();

        log.info("clientIp: {}", clientIp);

        User user = userRepository.findByUserLoginIdAndUserRoleCdAndIsDeletedFalse(testLoginReq.loginUserId(),
                UserRoleCd.ADMIN)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "사용자 정보가 없습니다."));

        adminPermissionRepository.findByIdUserNoAndIdIpAddressAndIsDeletedFalse(user.getUserNo(), clientIp)
                .stream()
                .findFirst()
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "허용되지 않은 IP 주소입니다: " + clientIp));

        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        return TestLoginRes.of(accessToken, refreshToken);
    }

    /**
     * 토큰 검증
     * 
     * @param token
     * @return
     */
    public Boolean checkToken(TestVerifyTokenReq testVerifyTokenReq) {
        return jwtUtil.isTokenValid(testVerifyTokenReq.token());
    }


    public List<EmailAccessLog> getSkTelecomEmailAccessLogByMybatis() {
        DataSourceContextHolder.setDataSourceKey("sktelecom");
        return skTelecomEmailAccessLogMapper.getEmailAccessLogs();
    }

    public List<EmailAccessLog> getHynixEmailAccessLogByMybatis() {
        DataSourceContextHolder.setDataSourceKey("hynix");
        return hynixEmailAccessLogMapper.getHynixEmailAccessLogs();
    }

    public String getToken(String loginUserId, UserRoleCd role) {
        User user = userRepository.findByUserLoginIdAndUserRoleCdAndIsDeletedFalse(loginUserId, role)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보가 없습니다."));
        return jwtUtil.generateAccessToken(user);
    }

    public String delete(Integer id) {
        CustomerCompany customerCompany = customerCompanyRepository.findByCustomerCompanyNoAndIsDeletedFalse(id)
                .orElseThrow();
        customerCompany.softDelete();
        return "ok";
    }

}
