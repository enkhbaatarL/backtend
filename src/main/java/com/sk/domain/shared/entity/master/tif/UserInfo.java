package com.sk.domain.shared.entity.master.tif;

import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;

import com.sk.domain.shared.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "tif_usrinfor")
@Comment("TIF_사용자정보수신")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserInfo extends BaseEntity {

    @Id
    @Column(name = "imcd", nullable = false, length = 100)
    @Comment("IM코드")
    private String imCd;

    @Column(name = "usr_nm", nullable = false, length = 100)
    @Comment("사용자명")
    private String userNm;

    @Column(name = "usr_engst_nm", length = 100)
    @Comment("사용자영문명")
    private String userEnglishNm;

    @Column(name = "logn_id", length = 50)
    @Comment("로그인ID")
    private String loginId;

    @Column(name = "emlad", length = 50)
    @Comment("이메일주소")
    private String emailAddress;

    @Column(name = "cpny_cd", nullable = false, length = 30)
    @Comment("회사코드")
    private String companyCd;

    @Column(name = "cpny_nm", nullable = false, length = 100)
    @Comment("회사명")
    private String companyNm;

    @Column(name = "emp_no", nullable = false, length = 50)
    @Comment("사원번호")
    private String employeeNo;

    @Column(name = "dept_cd", length = 30)
    @Comment("부서코드")
    private String departmentCd;

    @Column(name = "dept_nm", length = 100)
    @Comment("부서명")
    private String departmentNm;

    @Column(name = "ppstn_cd", length = 10)
    @Comment("직위코드")
    private String positionCd;

    @Column(name = "ppstn_nm", length = 50)
    @Comment("직위명")
    private String positionNm;

    @Column(name = "jbgrd_cd", length = 10)
    @Comment("직급코드")
    private String jobGradeCd;

    @Column(name = "jbgrd_nm", length = 50)
    @Comment("직급명")
    private String jobGradeNm;

    @Column(name = "ondt_cd", length = 10)
    @Comment("직책코드")
    private String onDutyCd;

    @Column(name = "ondt_nm", length = 50)
    @Comment("직책명")
    private String onDutyNm;

    @Builder
    public UserInfo(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId,
            Boolean isDeleted, String imCd, String userNm, String userEnglishNm, String loginId,
            String emailAddress, String companyCd, String companyNm, String employeeNo,
            String departmentCd, String departmentNm, String positionCd, String positionNm,
            String jobGradeCd, String jobGradeNm, String onDutyCd, String onDutyNm) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.imCd = imCd;
        this.userNm = userNm;
        this.userEnglishNm = userEnglishNm;
        this.loginId = loginId;
        this.emailAddress = emailAddress;
        this.companyCd = companyCd;
        this.companyNm = companyNm;
        this.employeeNo = employeeNo;
        this.departmentCd = departmentCd;
        this.departmentNm = departmentNm;
        this.positionCd = positionCd;
        this.positionNm = positionNm;
        this.jobGradeCd = jobGradeCd;
        this.jobGradeNm = jobGradeNm;
        this.onDutyCd = onDutyCd;
        this.onDutyNm = onDutyNm;
    }
}