package com.sk.domain.shared.entity.master.tmd;

import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.admin.dto.dataResult.UserWithCompany;
import com.sk.domain.admin.dto.request.AdminReq;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.enums.UserRoleCd;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Comment("TMD_사용자기본")
@Table(name = "tmd_usrm", indexes = {
        @Index(name = "TMD_USRM_IX01", columnList = "cust_cpny_no"),
        @Index(name = "TMD_USRM_IX02", columnList = "usr_logn_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usr_no", nullable = false, columnDefinition = "int unsigned")
    @Comment("사용자번호")
    private Integer userNo;

    @Column(name = "usr_nm", nullable = false, length = 100)
    @Comment("사용자명")
    private String userNm;

    @Column(name = "usr_logn_id", length = 50)
    @Comment("사용자로그인ID")
    private String userLoginId;

    @Enumerated(EnumType.STRING)
    @Column(name = "usr_role_cd", length = 10)
    @Comment("사용자역할코드")
    private UserRoleCd userRoleCd;

    @Column(name = "ppstn_nm", length = 50)
    @Comment("직위명")
    private String positionNm;

    @Column(name = "jbgrd_nm", length = 50)
    @Comment("직급명")
    private String jobGradeNm;

    @Column(name = "ondt_nm", length = 100)
    @Comment("직책명")
    private String onDutyNm;

    @Column(name = "dept_nm", length = 100)
    @Comment("부서명")
    private String departmentNm;

    @Column(name = "otcmp_svc_accs_psbl_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("타사서비스접근가능여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isOtherServiceAccessPossible;

    @Column(name = "ind_info_agmt_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("개인정보동의여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isIndividualInfoAgreement;

    @Column(name = "ind_info_agmt_dtm")
    @Comment("개인정보동의일시")
    private LocalDateTime individualInfoAgreementDtm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cust_cpny_no", referencedColumnName = "cust_cpny_no", nullable = false)
    @Comment("고객회사번호")
    @JsonBackReference
    private CustomerCompany customerCompany;

    @Builder
    public User(Integer userNo, String userNm, String userLoginId, UserRoleCd userRoleCd,
            String positionNm, String jobGradeNm, String onDutyNm, String departmentNm,
            Boolean isOtherServiceAccessPossible, Boolean isIndividualInfoAgreement,
            LocalDateTime individualInfoAgreementDtm, CustomerCompany customerCompany) {
        this.userNo = userNo;
        this.userNm = userNm;
        this.userLoginId = userLoginId;
        this.userRoleCd = userRoleCd;
        this.positionNm = positionNm;
        this.jobGradeNm = jobGradeNm;
        this.onDutyNm = onDutyNm;
        this.departmentNm = departmentNm;
        this.isOtherServiceAccessPossible = isOtherServiceAccessPossible;
        this.isIndividualInfoAgreement = isIndividualInfoAgreement;
        this.individualInfoAgreementDtm = individualInfoAgreementDtm;
        this.customerCompany = customerCompany;
    }

    public static User of(UserWithCompany userWithCompany, UserRoleCd userRoleCd) {
        return User.builder()
                .userNm(userWithCompany.userNm())
                .userLoginId(userWithCompany.userLoginId())
                .userRoleCd(userRoleCd)
                .positionNm(userWithCompany.positionNm() != null ? userWithCompany.positionNm() : null)
                .jobGradeNm(userWithCompany.jobGradeNm() != null ? userWithCompany.jobGradeNm() : null)
                .onDutyNm(userWithCompany.onDutyNm() != null ? userWithCompany.onDutyNm() : null)
                .departmentNm(userWithCompany.departmentNm() != null ? userWithCompany.departmentNm() : null)
                .isOtherServiceAccessPossible(false)
                .isIndividualInfoAgreement(false)
                .individualInfoAgreementDtm(null)
                .customerCompany(userWithCompany.customerCompany())
                .build();
    }

    public void update(AdminReq.Update dto){
        if(dto.isOtherServiceAccessPossible() != null) {
            this.isOtherServiceAccessPossible = dto.isOtherServiceAccessPossible();
        }
    }
}
