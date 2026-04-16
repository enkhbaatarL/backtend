package com.sk.domain.shared.entity.master.tmd;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.common.utils.DateUtil;
import com.sk.domain.admin.dto.request.company.CompanyReq;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.listener.CustomerCompanyListener;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Comment("TMD_고객회사기본")
@Table(name = "tmd_custcpnym")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(CustomerCompanyListener.class)
public class CustomerCompany extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cust_cpny_no", nullable = false, columnDefinition = "mediumint unsigned")
    @Comment("고객회사번호")
    private Integer customerCompanyNo;

    @Column(name = "cust_cpny_nm", nullable = false, length = 100)
    @Comment("고객회사명")
    private String customerCompanyNm;

    @Column(name = "cust_cpny_cd", nullable = false, length = 30)
    @Comment("고객회사코드")
    private String customerCompanyCd;

    @Column(name = "mrkt_idx_anly_tgt_cd", length = 15)
    @Comment("시장지수분석대상코드")
    private String marketIndexAnalysisTargetCd;

    @Column(name = "use_bgn_dt", length = 8)
    @Comment("사용시작일자")
    private String useBeginDt;

    @Column(name = "use_end_dt", length = 8)
    @Comment("사용종료일자")
    private String useEndDt;

    @Column(name = "use_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("사용여부")
    @ColumnDefault("'Y'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isUsed;

    @Column(name = "mip_mng_cpny_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("MI플랫폼관리회사여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isMipManagementCompany;

    @Column(name = "cust_cpny_mip_urlad", length = 255)
    @Comment("고객회사MI플랫폼URL주소")
    private String customerCompanyMipUrl;

    /**
     * 요청 호스트에 따라 동적인 MIP URL을 반환한다.
     *
     * @param requestHost 요청 호스트 (예: dev-mi.sk.com)
     * @param miDefaultDomain 기본 도메인
     * @return 결정된 MIP URL
     */
    public String getDynamicMipUrl(String requestHost, String miDefaultDomain) {
        return getDynamicMipUrlInternal(requestHost, miDefaultDomain, null);
    }

    /**
     * 요청 호스트에 따라 동적인 MIP URL을 반환한다.
     * 설정값(configMipUrl)이 있으면 설정값을 우선 사용하고, 없으면 DB 값을 사용한다.
     *
     * @param requestHost 요청 호스트 (예: dev-mi.sk.com)
     * @param miDefaultDomain 기본 도메인
     * @param configMipUrl 설정 파일의 MIP URL (로컬 테스트용, 회사코드로 조회된 URL)
     * @return 결정된 MIP URL
     */
    public String getDynamicMipUrl(String requestHost, String miDefaultDomain, String configMipUrl) {
        return getDynamicMipUrlInternal(requestHost, miDefaultDomain, configMipUrl);
    }

    /**
     * 내부 구현: 동적인 MIP URL을 반환한다.
     */
    private String getDynamicMipUrlInternal(String requestHost, String miDefaultDomain, String configMipUrl) {
        // 설정값이 있으면 설정값을 우선 사용
        if (configMipUrl != null && !configMipUrl.isBlank()) {
            return configMipUrl;
        }

        if (this.customerCompanyMipUrl == null || this.customerCompanyMipUrl.isBlank()) {
            return miDefaultDomain;
        }

        String[] parts = this.customerCompanyMipUrl.split(",");

        // A=B, C=D 형식인지 확인
        boolean isKeyValuePairFormat = true;
        for (String part : parts) {
            if (!part.contains("=")) {
                isKeyValuePairFormat = false;
                break;
            }
        }

        if (isKeyValuePairFormat) {
            for (String part : parts) {
                String[] kv = part.split("=");
                if (kv.length == 2) {
                    String key = kv[0].trim();
                    String value = kv[1].trim();
                    if (key.equals(requestHost)) {
                        return value;
                    }
                }
            }
            // 매칭되는 호스트가 없으면 첫 번째 쌍의 값을 반환하거나 기존 로직으로 진행할 수 있음.
            // 사용자 요청에 따라 매칭되는 것이 없으면 기존처럼 진행하도록 함.
        }

        // 기존 로직: 콤마로 구분된 여러 URL이 있는 경우 처리
        if (parts.length == 1) {
            return parts[0].trim();
        }

        // 호스트가 포함된 URL을 찾음
        for (String url : parts) {
            String trimmedUrl = url.trim();
            if (trimmedUrl.contains(requestHost)) {
                return trimmedUrl;
            }
        }

        // 매칭되는 호스트가 없으면 첫 번째 URL 반환
        return parts[0].trim();
    }

    @Column(name = "qlttv_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("정성모델 표시여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isQualitative;

    @Column(name = "mari_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("마리 표시여부")
    @ColumnDefault("'Y'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isMari;

    @Column(name = "future_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("미래 기능 예시 표출 여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isFuture;

    @Column(name = "nws_ltr_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("뉴스레터 화면 표출 여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isNewsLetter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "defl_land_scrn_elmnt_no")
    @Comment("기본랜딩화면요소번호")
    private ScreenElement defaultLandingScreenElement;

    @OneToMany(mappedBy = "customerCompany", fetch = FetchType.LAZY)
    private List<User> users = new ArrayList<>();

    @Builder
    public CustomerCompany(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId,
        Boolean isDeleted, Integer customerCompanyNo, String customerCompanyNm, String customerCompanyCd,
        String marketIndexAnalysisTargetCd, String useBeginDt, String useEndDt, Boolean isUsed, Boolean isMipManagementCompany,
        Boolean isQualitative, Boolean isMari, Boolean isFuture, Boolean isNewsLetter) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.customerCompanyNo = customerCompanyNo;
        this.customerCompanyNm = customerCompanyNm;
        this.customerCompanyCd = customerCompanyCd;
        this.marketIndexAnalysisTargetCd = marketIndexAnalysisTargetCd;
        this.useBeginDt = useBeginDt;
        this.useEndDt = useEndDt;
        this.isUsed = isUsed;
        this.isMipManagementCompany = isMipManagementCompany;
        this.isQualitative = isQualitative;
        this.isMari = isMari;
        this.isFuture = isFuture;
        this.isNewsLetter = isNewsLetter;
    }

    public static CustomerCompany ofOwner(CompanyReq.OwnerInsert dto, String companyNm) {
        return CustomerCompany.builder()
            .customerCompanyCd(dto.companyCd())
            .customerCompanyNm(companyNm)
            .isUsed(Boolean.TRUE)
            .isMipManagementCompany(Boolean.TRUE)
            .build();
    }

    public static CustomerCompany of(CompanyReq.Insert dto, String companyNm) {
        return CustomerCompany.builder()
            .customerCompanyCd(dto.companyCd())
            .customerCompanyNm(companyNm)
            .useBeginDt(DateUtil.toYmd(dto.usageBeginDate()))
            .useEndDt(DateUtil.toYmd(dto.usageEndDate()))
            .isUsed(Boolean.TRUE)
            .isMipManagementCompany(Boolean.FALSE)
            .build();
    }

    public void update(CompanyReq.Update companyReq) {

        if (ObjectUtils.isNotEmpty(companyReq.isActivated())) {
            this.isUsed = companyReq.isActivated();
        }

        if (ObjectUtils.isNotEmpty(companyReq.usageBeginDate())) {
            this.useBeginDt = DateUtil.toYmd(companyReq.usageBeginDate());
        }

        if (ObjectUtils.isNotEmpty(companyReq.usageEndDate())) {
            this.useEndDt = DateUtil.toYmd(companyReq.usageEndDate());
        }

    }

}
