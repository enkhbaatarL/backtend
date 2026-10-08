package com.sk.domain.shared.repository.master.tmd.custom;

import com.sk.domain.admin.dto.response.mcp.McpAuthInfoRes;
import org.springframework.data.domain.Page;

/** [MCP 인증] MCP 인증 키 목록 조회 (등록·수정자 정보 포함). */
public interface CustomMcpAuthInfoRepository {

    Page<McpAuthInfoRes> findMcpAuthInfoByConditions(int page, int size);

}
