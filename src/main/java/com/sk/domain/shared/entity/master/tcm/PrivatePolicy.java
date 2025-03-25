package com.sk.domain.shared.entity.master.tcm;

import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.shared.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "tcm_poltacm")
@Comment("TCM_정책약관기본")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PrivatePolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "poltac_no", nullable = false, columnDefinition = "int unsigned")
    @Comment("정책약관번호")
    private Integer poltacNo;

    @Column(name = "poltac_spt_cd", length = 2)
    @Comment("정책약관구분코드")
    private String poltacSeparateCd;

    @Column(name = "poltac_ver_nm", length = 20)
    @Comment("정책약관버전명")
    private String poltacVersionNm;

    @Column(name = "poltac_enfm_dtm", updatable = false, nullable = false)
    @Comment("정책약관시행일시")
    @ColumnDefault("CURRENT_TIMESTAMP")
    protected LocalDateTime poltacEnforcementDt;

    @Column(name = "poltac_cntt", columnDefinition = "text")
    @Comment("정책약관내용")
    private String poltacCntt;

    @Column(name = "use_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("사용여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isUsed;

}
