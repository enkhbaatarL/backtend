package com.sk.domain.shared.entity.master.tif;

import com.sk.domain.admin.dto.request.client.ClientAccessInfoReq;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import io.micrometer.common.util.StringUtils;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Getter
@Entity
@Comment("TIF_클라이언트접속정보기본")
@Table(name = "tif_clntaccsinfom")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClientAccessInfo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "clnt_accs_reg_no", nullable = false, columnDefinition = "smallint unsigned")
    @Comment("클라이언트접속등록번호")
    private Integer clientAccessRegNo;

    @Column(name = "clnt_cert_id", nullable = false, length = 255)
    @Comment("클라이언트인증ID")
    private String clientCertId;

    @Column(name = "clnts_kval", nullable = false, length = 255)
    @Comment("클라이언트시크릿키값")
    private String clientSecretKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cust_cpny_no", referencedColumnName = "cust_cpny_no")
    @Comment("고객회사")
    private CustomerCompany customerCompany;

    @Column(name = "cert_type_nm", length = 50)
    @Comment("인증유형명")
    private String certTypeNm;

    @Column(name = "cert_mthd_nm", length = 50)
    @Comment("인증방법명")
    private String certMethodNm;

    @Column(name = "pmsn_auth_scop_nm", length = 20)
    @Comment("허용권한범위명")
    private String permissionAuthScopeNm;

    @Builder
    public ClientAccessInfo(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId, Boolean isDeleted, Integer clientAccessRegNo, String clientCertId, String clientSecretKey, CustomerCompany customerCompany, String certTypeNm, String certMethodNm, String permissionAuthScopeNm) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.clientAccessRegNo = clientAccessRegNo;
        this.clientCertId = clientCertId;
        this.clientSecretKey = clientSecretKey;
        this.customerCompany = customerCompany;
        this.certTypeNm = certTypeNm;
        this.certMethodNm = certMethodNm;
        this.permissionAuthScopeNm = permissionAuthScopeNm;
    }

    public static ClientAccessInfo of(ClientAccessInfoReq.Insert dto, String encryptedClientSecret, CustomerCompany customerCompany) {
        return ClientAccessInfo.builder()
            .clientCertId(dto.clientCertId())
            .clientSecretKey(encryptedClientSecret)
            .customerCompany(customerCompany)
            .certTypeNm("client_credentials")
            .certMethodNm("client_secret_basic")
            .permissionAuthScopeNm("")
            .build();
    }

    public void update(String encryptedClientSecret) {
        this.clientSecretKey = encryptedClientSecret;
    }

}
