package com.sk.domain.shared.entity.master.tmd;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(name = "cpny_sbcm_spt_cd", length = 20)
    @Comment("회사분과구분코드")
    private String companySubcommitteeSeparateCd;

    @Column(name = "use_bgn_dt", length = 8)
    @Comment("사용시작일자")
    private String useBeginDt;

    @Column(name = "use_end_dt", length = 8)
    @Comment("사용종료일자")
    private String useEndDt;

    @Column(name = "use_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("사용여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isUsed;

    @OneToMany(mappedBy = "customerCompany", fetch = FetchType.LAZY)
    private List<User> users = new ArrayList<>();

    @Builder
    public CustomerCompany(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId, Boolean isDeleted,
            Integer customerCompanyNo, String customerCompanyNm, String customerCompanyCd, String useBeginDt, String useEndDt, Boolean isUsed) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.customerCompanyNo = customerCompanyNo;
        this.customerCompanyNm = customerCompanyNm;
        this.customerCompanyCd = customerCompanyCd;
        this.useBeginDt = useBeginDt;
        this.useEndDt = useEndDt;
        this.isUsed = isUsed;
    }

    public static CustomerCompany of(CompanyReq.Insert dto) {
        return CustomerCompany.builder()
                .customerCompanyCd(dto.companyCd())
                .customerCompanyNm(dto.companyNm())
                .useBeginDt(DateUtil.toYmd(dto.usageBeginDate()))
                .useEndDt(DateUtil.toYmd(dto.usageEndDate()))
                .isUsed(Boolean.TRUE)
                .build();
    }

    public void update(CompanyReq.Update companyReq) {

        if (ObjectUtils.isNotEmpty(companyReq.isActivated())) {
            this.isUsed = companyReq.isActivated();
        }

        if (ObjectUtils.isNotEmpty(companyReq.companyNm())) {
            this.customerCompanyNm = companyReq.companyNm();
        }

        if (ObjectUtils.isNotEmpty(companyReq.usageBeginDate())) {
            this.useBeginDt = DateUtil.toYmd(companyReq.usageBeginDate());
        }

        if (ObjectUtils.isNotEmpty(companyReq.usageEndDate())) {
            this.useEndDt = DateUtil.toYmd(companyReq.usageEndDate());
        }

    }

}
