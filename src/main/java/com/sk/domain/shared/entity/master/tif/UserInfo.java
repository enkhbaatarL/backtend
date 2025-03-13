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
    private String imCode;

    @Column(name = "usr_nm", nullable = false, length = 100)
    @Comment("사용자명")
    private String userName;

    @Column(name = "usr_engst_nm", length = 100)
    @Comment("사용자영문명")
    private String userEnglishName;

    @Column(name = "logn_id", length = 50)
    @Comment("로그인ID")
    private String loginId;

    @Column(name = "emlad", length = 50)
    @Comment("이메일주소")
    private String emailAddress;

    @Column(name = "cpny_cd", nullable = false, length = 30)
    @Comment("회사코드")
    private String companyCode;

    @Column(name = "cpny_nm", nullable = false, length = 100)
    @Comment("회사명")
    private String companyName;

    @Column(name = "emp_no", nullable = false, length = 50)
    @Comment("사원번호")
    private String employeeNumber;

    @Column(name = "dept_cd", length = 30)
    @Comment("부서코드")
    private String departmentCode;

    @Column(name = "dept_nm", length = 100)
    @Comment("부서명")
    private String departmentName;

    @Column(name = "ppstn_cd", length = 10)
    @Comment("직위코드")
    private String positionCode;

    @Column(name = "ppstn_nm", length = 50)
    @Comment("직위명")
    private String positionName;

    @Column(name = "jbgrd_cd", length = 10)
    @Comment("직급코드")
    private String jobGradeCode;

    @Column(name = "jbgrd_nm", length = 50)
    @Comment("직급명")
    private String jobGradeName;

    @Column(name = "ondt_cd", length = 10)
    @Comment("직책코드")
    private String onDutyCode;

    @Column(name = "ondt_nm", length = 50)
    @Comment("직책명")
    private String onDutyName;

    @Builder
    public UserInfo(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId,
            Boolean isDeleted, String imCode, String userName, String userEnglishName, String loginId,
            String emailAddress, String companyCode, String companyName, String employeeNumber,
            String departmentCode, String departmentName, String positionCode, String positionName,
            String jobGradeCode, String jobGradeName, String onDutyCode, String onDutyName) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.imCode = imCode;
        this.userName = userName;
        this.userEnglishName = userEnglishName;
        this.loginId = loginId;
        this.emailAddress = emailAddress;
        this.companyCode = companyCode;
        this.companyName = companyName;
        this.employeeNumber = employeeNumber;
        this.departmentCode = departmentCode;
        this.departmentName = departmentName;
        this.positionCode = positionCode;
        this.positionName = positionName;
        this.jobGradeCode = jobGradeCode;
        this.jobGradeName = jobGradeName;
        this.onDutyCode = onDutyCode;
        this.onDutyName = onDutyName;
    }
}