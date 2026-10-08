package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.McpAuthInfo;
import com.sk.domain.shared.repository.master.tmd.custom.CustomMcpAuthInfoRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** [MCP 인증] MCP 인증 키 저장소. */
public interface McpAuthInfoRepository extends JpaRepository<McpAuthInfo, Integer>, CustomMcpAuthInfoRepository {

    @EntityGraph(attributePaths = {"customerCompany"})
    Optional<McpAuthInfo> findByMcpAuthRegNoAndIsDeletedFalse(Integer mcpAuthRegNo);

    @EntityGraph(attributePaths = {"customerCompany"})
    Optional<McpAuthInfo> findByMcpAuthIdAndIsDeletedFalse(String mcpAuthId);

    /**
     * 삭제된 행은 ID 가 d_ 로 바뀌어 있으므로 삭제 여부와 관계없이 조회해도 재등록을 막지 않습니다.
     * DB UNIQUE 제약과 같은 기준(전체 행)으로 검사합니다.
     */
    boolean existsByMcpAuthId(String mcpAuthId);

}
