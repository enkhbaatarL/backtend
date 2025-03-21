package com.sk.domain.admin.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sk.common.config.database.dataSource.DataSourceContextHolder;
import com.sk.common.utils.JwtUtil;
import com.sk.domain.admin.dto.request.TestReq;
import com.sk.domain.admin.dto.response.TestRes;
import com.sk.domain.shared.entity.master.tlm.EmailAccessLog;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.UserRoleCd;
import com.sk.domain.shared.mapper.hynix.tlm.HynixEmailAccessLogMapper;
import com.sk.domain.shared.mapper.master.tlm.MasterEmailAccessLogMapper;
import com.sk.domain.shared.repository.master.tmd.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestService {

    private final JwtUtil jwtUtil;
    private final MasterEmailAccessLogMapper masterEmailAccessLogMapper;
    private final HynixEmailAccessLogMapper hynixEmailAccessLogMapper;
    private final UserRepository userRepository;

    public TestRes getUserInfo(TestReq testReq) {
        return TestRes.of(testReq);
    }

    public Boolean userLogin() {
        return true;
    }

    public Boolean adminLogin() {
        return true;
    }

    public List<EmailAccessLog> getMasterEmailAccessLogByMybatis() {
        DataSourceContextHolder.setDataSourceKey("master");
        return masterEmailAccessLogMapper.getEmailAccessLogs();
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

}
