package com.sk.domain.shared.entity.master.tlm;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.compositeKey.tlm.EconomicCalendarInquiryLogId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Table(name = "tlm_ecncalinqrusrl")
@IdClass(EconomicCalendarInquiryLogId.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicInsert
public class EconomicCalendarInquiryLog extends BaseEntity {

    @Id
    @Column(name = "usr_no")
    private Integer userNo;

    @Id
    @Column(name = "ecn_cal_code", length = 20)
    private String economicCalendarCode;

    @Id
    @Column(name = "inqr_dtm", precision = 6)
    private LocalDateTime inquiryDateTime;

    public static EconomicCalendarInquiryLog createLog(int userNo, String economicCalendarCode) {
        EconomicCalendarInquiryLog log = new EconomicCalendarInquiryLog();
        log.setUserNo(userNo);
        log.setEconomicCalendarCode(economicCalendarCode);
        log.setInquiryDateTime(LocalDateTime.now());
        return log;
    }
}
