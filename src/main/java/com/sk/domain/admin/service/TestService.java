package com.sk.domain.admin.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import com.sk.common.config.database.dataSource.DataSourceContextHolder;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.JwtUtil;
import com.sk.domain.admin.dto.request.TestReq;
import com.sk.domain.admin.dto.response.TestLoginRes;
import com.sk.domain.admin.dto.response.TestRes;
import com.sk.domain.admin.dto.response.file.FileUploadRes;
import com.sk.domain.shared.entity.master.tcm.ComponentInfo;
import com.sk.domain.shared.entity.master.tlm.EmailAccessLog;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.UserRoleCd;
import com.sk.domain.shared.mapper.hynix.tlm.HynixEmailAccessLogMapper;
import com.sk.domain.shared.mapper.sktelecom.tlm.SkTelecomEmailAccessLogMapper;
import com.sk.domain.shared.repository.master.tam.AdminPermissionRepository;
import com.sk.domain.shared.repository.master.tcm.ComponentInfoRepository;
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
    private final ComponentInfoRepository componentInfoRepository;
    private final ExcelService excelService;

    public TestRes.GetUserInfo getUserInfo(TestReq.getUserInfo testReq) {
        return TestRes.GetUserInfo.of(testReq);
    }

    /**
     * 사용자 로그인
     * 
     * @param testLoginReq
     * @return
     */
    @Transactional
    public TestLoginRes userLogin(TestReq.userLogin testLoginReq) {
        User user = userRepository.findByUserLoginIdAndUserRoleCdAndIsDeletedFalse(testLoginReq.loginUserId(),
                UserRoleCd.USER)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                        "User information could not be found. userLoginId: " + testLoginReq.loginUserId()));

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
    public TestLoginRes adminLogin(TestReq.adminLogin testLoginReq) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        String clientIp = request.getRemoteAddr();

        log.info("clientIp: {}", clientIp);

        User user = userRepository.findByUserLoginIdAndUserRoleCdAndIsDeletedFalse(testLoginReq.loginUserId(),
                testLoginReq.userRoleCd())
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format(
                        "User information could not be found. userLoginId: %s, userRoleCd: %s", testLoginReq.loginUserId(), testLoginReq.userRoleCd())));

        adminPermissionRepository.findByIdUserNoAndIdIpAddressAndIsDeletedFalse(user.getUserNo(), clientIp)
                .stream()
                .findFirst()
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("The IP address is not allowed: %s", clientIp)));

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
    public Boolean checkToken(TestReq.verifyToken testVerifyTokenReq) {
        return jwtUtil.isTokenValid(testVerifyTokenReq.token());
    }

    public List<EmailAccessLog> getSkTelecomEmailAccessLogByMybatis() {
        DataSourceContextHolder.setDataSourceKey("SKT");
        return skTelecomEmailAccessLogMapper.getEmailAccessLogs();
    }

    public List<EmailAccessLog> getHynixEmailAccessLogByMybatis() {
        DataSourceContextHolder.setDataSourceKey("SKCC");
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

    /**
     * 정형 데이터 파일 검수 후 업로드
     * 
     * @param file
     * @param testReq
     * @return
     */
    public FileUploadRes uploadStructuredDataFile(MultipartFile file, TestReq.uploadStructuredDataFile testReq) {

        // 컴포넌트 타입 조회
        ComponentInfo componentInfo = componentInfoRepository.findByComponentTypeIdAndIsDeletedFalse(testReq.componentTypeId())
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                        String.format("Component information could not be found. componentTypeId: %s", testReq.componentTypeId())));

        // TODO: DataSourceContextHolder 설정

        // 컴포넌트 엑셀 칼럼 추출
        String columnName = componentInfo.getExcelColumnListNm();

        if (columnName == null || columnName.isBlank()) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Column name is null or blank");
        }

        String[] columnNames = Arrays.stream(columnName.split(",")).map(String::trim).filter(name -> !name.isEmpty()).toArray(String[]::new);

        // 엑셀 파일 검수 후 업로드
        // excelService.validateStructuredExcelData(file, columnNames, componentInfo.getTargetTableNm(), componentInfo.getTargetDbNm());

        //TODO: 데아터 소스 저장, 

        return null;
    }

    /**
     * 정형 데이터 컴포넌트 저장
     * 
     * @param testReq
     * @return
     */
    public TestRes.SaveComponent saveComponent(TestReq.saveComponent testReq) {
        ComponentInfo componentInfo = componentInfoRepository.save(ComponentInfo.of(testReq));

        //데이터칼럼 설명 명 데이터 업로드

        return TestRes.SaveComponent.of(componentInfo);
    }
}
