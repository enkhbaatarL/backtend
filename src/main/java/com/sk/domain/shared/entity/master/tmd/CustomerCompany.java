package com.sk.domain.shared.entity.master.tmd;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import com.sk.domain.shared.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;

@Getter
@Entity
@Comment("TMD_고객회사기본")
@Table(name = "tmd_custcpnym")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Column(name = "use_bgn_dt", length = 8)
    @Comment("사용시작일자")
    private String useBeginDt;

    @Column(name = "use_end_dt", length = 8)
    @Comment("사용종료일자")
    private String useEndDt;

    @Builder
    public CustomerCompany(Integer customerCompanyNo, String customerCompanyNm, String customerCompanyCd,
            String useBeginDt, String useEndDt) {
        this.customerCompanyNo = customerCompanyNo;
        this.customerCompanyNm = customerCompanyNm;
        this.customerCompanyCd = customerCompanyCd;
        this.useBeginDt = useBeginDt;
        this.useEndDt = useEndDt;
    }
}
