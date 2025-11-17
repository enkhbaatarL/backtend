package com.sk.domain.shared.entity.master.tmd;

import com.sk.common.converter.SignificanceLevelAttributeConverter;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.enums.ActivityLogType;
import com.sk.domain.shared.enums.SignificanceLevel;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Table(
    name = "tmd_actlogl",
    indexes = {
        @Index(name = "tmd_actlogl_ix_01", columnList = "cust_cpny_no, evnt_type")
    }
)
@Comment("TMD_활동기록내역")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "act_log_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("활동기록아이디")
    private Long actLogId;

    @Column(name = "cust_cpny_no", nullable = false, columnDefinition = "mediumint unsigned", insertable = false, updatable = false)
    @Comment("고객회사번호")
    private Integer customerCompanyNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cust_cpny_no", foreignKey = @jakarta.persistence.ForeignKey(name = "actlogl_fk01"))
    private CustomerCompany customerCompany;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crt_usr_logn_id", referencedColumnName = "usr_logn_id", insertable = false, updatable = false)
    @Comment("생성사용자")
    private User createUser;

    @Column(name = "evnt_dt", nullable = false, length = 8)
    @Comment("이벤트발생일자")
    private String evntDt;

    @Enumerated(EnumType.STRING)
    @Column(name = "evnt_type", nullable = false)
    @Comment("이벤트타입")
    private ActivityLogType evntType;

    @Column(name = "acct_nm", nullable = false, length = 100)
    @Comment("업체명")
    private String acctNm;

    @Column(name = "evnt_dtl_expn", nullable = false, length = 500)
    @Comment("이벤트상세설명")
    private String evntDtlExpn;

    @Column(name = "sig_lvl", nullable = false, columnDefinition = "tinyint")
    @Comment("중요도(3: High, 2: Medium, 1: Low)")
    @Convert(converter = SignificanceLevelAttributeConverter.class)
    private SignificanceLevel sigLvl;

    @Column(name = "src_nm", nullable = false, length = 100)
    @Comment("출처")
    private String srcNm;

    @Column(name = "dept_nm", nullable = false, length = 100)
    @Comment("소속팀")
    private String deptNm;

    @Builder
    public ActivityLog(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId,
            Boolean isDeleted, Long actLogId, CustomerCompany customerCompany, Integer customerCompanyNo, String evntDt, ActivityLogType evntType, String acctNm,
            String evntDtlExpn, SignificanceLevel sigLvl, String srcNm, String deptNm) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.actLogId = actLogId;
        this.customerCompany = customerCompany;
        this.customerCompanyNo = customerCompanyNo;
        this.evntDt = evntDt;
        this.evntType = evntType;
        this.acctNm = acctNm;
        this.evntDtlExpn = evntDtlExpn;
        this.sigLvl = sigLvl;
        this.srcNm = srcNm;
        this.deptNm = deptNm;
    }

    /**
     * ActivityLog 생성 정적 팩토리 메서드
     */
    public static ActivityLog of(CustomerCompany customerCompany, Integer customerCompanyNo, String evntDt, ActivityLogType evntType, String acctNm, String evntDtlExpn,
            SignificanceLevel sigLvl, String srcNm, String deptNm) {
        return ActivityLog.builder()
                .customerCompany(customerCompany)
                .customerCompanyNo(customerCompanyNo)
                .evntDt(evntDt)
                .evntType(evntType)
                .acctNm(acctNm)
                .evntDtlExpn(evntDtlExpn)
                .sigLvl(sigLvl)
                .srcNm(srcNm)
                .deptNm(deptNm)
                .build();
    }
}
