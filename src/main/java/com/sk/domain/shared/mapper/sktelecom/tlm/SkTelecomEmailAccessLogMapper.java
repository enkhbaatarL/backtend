package com.sk.domain.shared.mapper.sktelecom.tlm;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.sk.domain.shared.entity.master.tlm.EmailAccessLog;

@Mapper
public interface SkTelecomEmailAccessLogMapper {
    List<EmailAccessLog> getEmailAccessLogs();
}
