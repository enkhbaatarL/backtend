package com.sk.domain.shared.mapper.hynix.tlm;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.sk.domain.shared.entity.master.tlm.EmailAccessLog;

//TODO: 삭제 필요
@Mapper
public interface HynixEmailAccessLogMapper {
    List<EmailAccessLog> getHynixEmailAccessLogs();
}
