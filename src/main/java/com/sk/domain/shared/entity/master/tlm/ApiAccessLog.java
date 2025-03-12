package com.sk.domain.shared.entity.master.tlm;

import com.sk.domain.shared.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

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

    @Column(name = "usr_logn_id", nullable = false, length = 50)
    @Comment("사용자로그인ID")
    private String userLoginId;

    @Column(name = "api_endp_urlad", nullable = false, length = 255)
    @Comment("API엔드포인트URL주소")
    private String apiEndpointUrl;

    @Column(name = "httpm_nm", nullable = false, length = 10)
    @Comment("HTTP메서드명")
    private String httpMethodNm;

    @Lob
    @Column(name = "req_data_cntt", columnDefinition = "text")
    @Comment("요청데이터내용")
    private String requestDataContent;

    @Lob
    @Column(name = "err_msg_cntt", columnDefinition = "text")
    @Comment("오류메세지내용")
    private String errorMessageContent;

    @Builder
    public ApiAccessLog(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId,
                        Integer apiAccessSn, Boolean isDeleted, String userLoginId, String apiEndpointUrl,
                        String httpMethodNm, String requestDataContent, String errorMessageContent) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.apiAccessSn = apiAccessSn;
        this.userLoginId = userLoginId;
        this.apiEndpointUrl = apiEndpointUrl;
        this.httpMethodNm = httpMethodNm;
        this.requestDataContent = requestDataContent;
        this.errorMessageContent = errorMessageContent;
    }

    public static ApiAccessLog of(String uri, String httpMethodName, String userLoginId, String body, String errorMessageContent) {
        return ApiAccessLog.builder()
            .apiEndpointUrl(uri)
            .httpMethodNm(httpMethodName)
            .userLoginId(userLoginId)
            .requestDataContent(body)
            .errorMessageContent(errorMessageContent)
            .build();
    }

}

