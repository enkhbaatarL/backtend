package com.sk.domain.shared.entity.master.tmd;

import com.sk.common.config.security.mcp.McpSecretHash;
import com.sk.domain.shared.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/** [MCP 인증] MCP 인증 키 (tmd_mcpauthinfom). 시크릿은 PBKDF2 해시만 저장하며 기존 OAuth 키(tif_clntaccsinfom)와 별도 테이블. */
@Getter
@Entity
@Comment("TMD_MCP인증정보기본")
@Table(name = "tmd_mcpauthinfom")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class McpAuthInfo extends BaseEntity {

    private static final String DELETED_ID_PREFIX = "d_";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mcp_auth_reg_no", nullable = false, columnDefinition = "int unsigned")
    @Comment("MCP인증등록번호")
    private Integer mcpAuthRegNo;

    @Column(name = "mcp_auth_id", nullable = false, length = 70)
    @Comment("MCP인증ID")
    private String mcpAuthId;

    @Column(name = "scrt_hash_val", nullable = false, length = 100)
    @Comment("시크릿해시값")
    private String secretHashValue;

    @Column(name = "scrt_salt_val", nullable = false, length = 50)
    @Comment("시크릿솔트값")
    private String secretSaltValue;

    @Column(name = "hash_algr_nm", nullable = false, length = 30)
    @Comment("해시알고리즘명")
    private String hashAlgorithmNm;

    @Column(name = "hash_itrt_cnt", nullable = false, columnDefinition = "int unsigned")
    @Comment("해시반복횟수")
    private Integer hashIterationCnt;

    @Column(name = "scrt_prfx_val", nullable = false, length = 10)
    @Comment("시크릿앞자리값")
    private String secretPrefixValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cust_cpny_no", referencedColumnName = "cust_cpny_no", nullable = false)
    @Comment("고객회사")
    private CustomerCompany customerCompany;

    @Builder
    public McpAuthInfo(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId, Boolean isDeleted, Integer mcpAuthRegNo, String mcpAuthId, String secretHashValue, String secretSaltValue, String hashAlgorithmNm, Integer hashIterationCnt, String secretPrefixValue, CustomerCompany customerCompany) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.mcpAuthRegNo = mcpAuthRegNo;
        this.mcpAuthId = mcpAuthId;
        this.secretHashValue = secretHashValue;
        this.secretSaltValue = secretSaltValue;
        this.hashAlgorithmNm = hashAlgorithmNm;
        this.hashIterationCnt = hashIterationCnt;
        this.secretPrefixValue = secretPrefixValue;
        this.customerCompany = customerCompany;
    }

    public static McpAuthInfo of(String mcpAuthId, McpSecretHash secretHash, CustomerCompany customerCompany) {
        return McpAuthInfo.builder()
            .mcpAuthId(mcpAuthId)
            .secretHashValue(secretHash.hashValue())
            .secretSaltValue(secretHash.saltValue())
            .hashAlgorithmNm(secretHash.algorithmNm())
            .hashIterationCnt(secretHash.iterationCnt())
            .secretPrefixValue(secretHash.prefixValue())
            .customerCompany(customerCompany)
            .isDeleted(Boolean.FALSE)
            .build();
    }

    public void updateSecret(McpSecretHash secretHash) {
        this.secretHashValue = secretHash.hashValue();
        this.secretSaltValue = secretHash.saltValue();
        this.hashAlgorithmNm = secretHash.algorithmNm();
        this.hashIterationCnt = secretHash.iterationCnt();
        this.secretPrefixValue = secretHash.prefixValue();
    }

    /**
     * 논리 삭제합니다. 같은 ID 로 재등록할 수 있도록 ID 를 d_{등록번호}_{원래ID} 로 바꿉니다.
     * 입력 ID 는 '_' 를 허용하지 않으므로 사용자가 입력한 ID 와 겹치지 않습니다.
     */
    public void delete() {
        this.mcpAuthId = DELETED_ID_PREFIX + this.mcpAuthRegNo + "_" + this.mcpAuthId;
        softDelete();
    }

}
