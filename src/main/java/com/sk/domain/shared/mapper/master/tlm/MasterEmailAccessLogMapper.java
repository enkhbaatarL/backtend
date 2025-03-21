package com.sk.domain.shared.mapper.master.tlm;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.sk.domain.shared.entity.master.tlm.EmailAccessLog;

@Mapper
public interface MasterEmailAccessLogMapper {
    List<EmailAccessLog> getEmailAccessLogs();
}
