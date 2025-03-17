package com.sk.domain.shared.entity.master.tif;

import com.sk.domain.shared.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Getter
@Entity
@Comment("TIF_회사정보수신")
@Table(name = "tif_cpnyinfor")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyInfo extends BaseEntity {

    @Id
    @Column(name = "cpny_cd", nullable = false, length = 30)
    @Comment("회사코드")
    private String companyCd;

    @Column(name = "cpny_nm", nullable = false, length = 100)
    @Comment("회사명")
    private String companyNm;

    @Column(name = "cpny_engst_nm", length = 100)
    @Comment("회사영문명")
    private String companyEngNm;

    @Builder
    public CompanyInfo(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId, String companyCd, Boolean isDeleted, String companyNm, String companyEngNm) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.companyCd = companyCd;
        this.companyNm = companyNm;
        this.companyEngNm = companyEngNm;
    }

//    public static CompanyInfo of(CustomerCompanyReq customerCompanyReq) {
//        return CompanyInfo.builder()
//            .companyCd(customerCompanyReq.companyCd())
//            .companyNm(customerCompanyReq.companyNm())
//            .companyEngNm(customerCompanyReq.companyEngNm())
//            .isDeleted(Boolean.FALSE)
//            .build();
//    }

}
