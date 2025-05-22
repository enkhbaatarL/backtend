package com.sk.domain.shared.entity.master.tmd;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.common.utils.DateUtil;
import com.sk.domain.admin.dto.request.company.CompanyReq;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.listener.CustomerCompanyListener;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "customerCompany", fetch = FetchType.LAZY)
    private List<User> users = new ArrayList<>();

    @Builder
    public CustomerCompany(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId, Boolean isDeleted, Integer customerCompanyNo, String customerCompanyNm, String customerCompanyCd, String marketIndexAnalysisTargetCd, String useBeginDt, String useEndDt, Boolean isUsed, Boolean isMipManagementCompany) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.customerCompanyNo = customerCompanyNo;
        this.customerCompanyNm = customerCompanyNm;
        this.customerCompanyCd = customerCompanyCd;
        this.marketIndexAnalysisTargetCd = marketIndexAnalysisTargetCd;
        this.useBeginDt = useBeginDt;
        this.useEndDt = useEndDt;
        this.isUsed = isUsed;
        this.isMipManagementCompany = isMipManagementCompany;
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
