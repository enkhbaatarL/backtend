package com.sk.domain.shared.entity.master.tcm;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.shared.converter.StringToPolicyTacticsSeparateCdConverter;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.enums.PolicyTacticsSeparateCd;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "tcm_poltacm")
@Comment("TCM_정책약관기본")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PolicyTactics extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "poltac_no", nullable = false, columnDefinition = "int unsigned")
    @Comment("정책약관번호")
    private Integer poltacNo;

    @Column(name = "poltac_spt_cd", nullable = false, length = 2)
    @Comment("정책약관구분코드")
    @Convert(converter = StringToPolicyTacticsSeparateCdConverter.class)
    private PolicyTacticsSeparateCd poltacSeparateCd;

    @Column(name = "poltac_ver_nm", nullable = false, length = 20)
    @Comment("정책약관버전명")
    private String poltacVersionNm;

    @Column(name = "poltac_enfm_dtm", nullable = false, columnDefinition = "datetime")
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Comment("정책약관시행일시")
    private LocalDateTime poltacEnforcementDt;

    @Column(name = "poltac_cntt", nullable = false, columnDefinition = "text")
    @Comment("정책약관내용")
    private String poltacCntt;

    @Column(name = "use_yn", length = 1, columnDefinition = "char(1)")
    @Comment("사용여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isUsed;

}
