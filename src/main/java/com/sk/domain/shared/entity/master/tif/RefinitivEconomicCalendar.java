package com.sk.domain.shared.entity.master.tif;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.shared.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

/**
 * Refinitiv 경제 지표 달력 엔티티
 */
@Entity
@Table(name = "tif_refiecnmicclndrr",
       uniqueConstraints = {
           @UniqueConstraint(name = "tif_refiecnmicclndrr_ux01", columnNames = {"code"})
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("TIF_Refinitiv 경제 달력")
public class RefinitivEconomicCalendar extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "data_reg_sn", columnDefinition = "int unsigned")
    @Comment("데이터등록일련번호")
    private Integer dataRegistrationSn;

    @Column(name = "code", nullable = false, length = 20)
    @Comment("코드")
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    @Comment("이름")
    private String name;

    @Column(name = "prsnttn_rgn", nullable = false, length = 100)
    @Comment("발표지역")
    private String presentationRegion;

    @Column(name = "expn", length = 200)
    @Comment("설명")
    private String explanation;

    @Column(name = "src_org", nullable = false, length = 200)
    @Comment("소스기관")
    private String sourceOrganization;

    @Column(name = "type", length = 100)
    @Comment("유형")
    private String type;

    @Column(name = "unit", length = 30)
    @Comment("단위")
    private String unit;

    @Column(name = "prsnttn_cyc", length = 20)
    @Comment("발표주기")
    private String presentationCycle;

    @Column(name = "use_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("사용여부")
    @ColumnDefault("'Y'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isUsed;

    @Column(name = "priory", columnDefinition = "smallint unsigned")
    @Comment("우선순위")
    private Short priority;

    @Column(name = "prsnttn_dtm", length = 100)
    @Comment("발표일시")
    private String presentationDatetime;

    @Builder
    public RefinitivEconomicCalendar(String code, String name, String presentationRegion,
                                      String explanation, String sourceOrganization, String type,
                                      String unit, String presentationCycle, Boolean isUsed,
                                      Short priority, String presentationDatetime) {
        this.code = code;
        this.name = name;
        this.presentationRegion = presentationRegion;
        this.explanation = explanation;
        this.sourceOrganization = sourceOrganization;
        this.type = type;
        this.unit = unit;
        this.presentationCycle = presentationCycle;
        this.isUsed = isUsed;
        this.priority = priority;
        this.presentationDatetime = presentationDatetime;
    }

}
