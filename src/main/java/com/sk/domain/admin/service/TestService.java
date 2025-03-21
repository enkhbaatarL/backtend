package com.sk.domain.admin.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sk.common.config.database.dataSource.DataSourceContextHolder;
import com.sk.domain.admin.dto.request.TestReq;
import com.sk.domain.admin.dto.response.TestRes;
import com.sk.domain.shared.entity.master.tlm.EmailAccessLog;
import com.sk.domain.shared.mapper.hynix.tlm.HynixEmailAccessLogMapper;
import com.sk.domain.shared.mapper.master.tlm.MasterEmailAccessLogMapper;
import com.sk.domain.shared.service.MailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestService {

    private final MailService mailService;
    private final MasterEmailAccessLogMapper masterEmailAccessLogMapper;
    private final HynixEmailAccessLogMapper hynixEmailAccessLogMapper;

    public TestRes getUserInfo(TestReq testReq) {
        return TestRes.of(testReq);
    }

    public Boolean sendMail() {
        String to = "hoyoung@twolinecode.com";
        String subject = "Test Mail";
        String data = "asd";
        mailService.sendMail(to, subject, data);
        return true;
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
}
