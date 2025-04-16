package com.sk.domain.shared.entity.master.tlm;

import java.time.LocalDateTime;

import com.sk.domain.shared.enums.Role;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedBy;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.enums.UserRoleCd;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Comment("TLM_API접속내역")
@Table(name = "tlm_apiaccsl")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiAccessLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "api_accs_sn", nullable = false)
    @Comment("API접속일련번호")
    private Integer apiAccessSn;

    @CreatedBy
    @Column(name = "usr_logn_id", nullable = false, length = 50)
    @Comment("사용자로그인ID")
    private String userLoginId;

    @Column(name = "api_endp_urlad", nullable = false, length = 255)
    @Comment("API엔드포인트URL주소")
    private String apiEndpointUrl;

    @Column(name = "httpm_nm", nullable = false, length = 10)
    @Comment("HTTP메서드명")
    private String httpMethodNm;

    @Column(name = "accs_ipadr", length = 15)
    @Comment("접속아이피주소")
    private String accessIpAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "usr_role_cd", length = 10)
    @Comment("사용자역할코드")
    private String userRoleCd;

    @Lob
    @Column(name = "req_data_cntt", columnDefinition = "text")
    @Comment("요청데이터내용")
    private String requestDataCntt;

    @Lob
    @Column(name = "err_msg_cntt", columnDefinition = "text")
    @Comment("오류메세지내용")
    private String errorMessageCntt;

    @Builder
    public ApiAccessLog(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId,
                        Integer apiAccessSn, Boolean isDeleted, String userLoginId, String apiEndpointUrl,
                        String httpMethodNm, String requestDataCntt, String errorMessageCntt, String accessIpAddress,
                        String userRoleCd) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.apiAccessSn = apiAccessSn;
        this.userLoginId = userLoginId;
        this.apiEndpointUrl = apiEndpointUrl;
        this.httpMethodNm = httpMethodNm;
        this.accessIpAddress = accessIpAddress;
        this.userRoleCd = userRoleCd;
        this.requestDataCntt = requestDataCntt;
        this.errorMessageCntt = errorMessageCntt;
    }

    public static ApiAccessLog of(String uri, String httpMethodNm, String body, String errorMessageCntt, String accessIpAddress, Role role) {
        return ApiAccessLog.builder()
            .apiEndpointUrl(uri)
            .httpMethodNm(httpMethodNm)
            // .userLoginId(userLoginId)
            .requestDataCntt(body)
            .errorMessageCntt(errorMessageCntt)
            .accessIpAddress(accessIpAddress)
            .userRoleCd(role.getCode())
            .build();
    }

}

