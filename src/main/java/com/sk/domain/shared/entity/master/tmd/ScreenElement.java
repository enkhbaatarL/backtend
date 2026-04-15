package com.sk.domain.shared.entity.master.tmd;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.enums.ScreenElementType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Table(name = "tmd_scrnelmntm")
@Comment("TMD_화면요소기본")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScreenElement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scrn_elmnt_no", nullable = false, columnDefinition = "int unsigned")
    @Comment("화면요소번호")
    private Integer screenElementNo;

    @Column(name = "scrn_elmnt_cd", nullable = false, length = 30)
    @Comment("화면요소코드")
    private String screenElementCd;

    @Column(name = "scrn_elmnt_expln", length = 500)
    @Comment("화면요소설명")
    private String screenElementExpln;

    @Column(name = "scrn_elmnt_type_cd", nullable = false, length = 20)
    @Comment("화면요소유형코드")
    @Enumerated(EnumType.STRING)
    private ScreenElementType screenElementTypeCd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upr_scrn_elmnt_no")
    @Comment("상위화면요소번호")
    private ScreenElement upperScreenElement;

    @Column(name = "defl_ord_no", columnDefinition = "smallint unsigned")
    @Comment("기본정렬순서")
    private Integer defaultOrderNo;

    @Column(name = "defl_vsblty_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("기본노출여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isDefaultExposed;

    @Column(name = "use_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("사용여부")
    @ColumnDefault("'Y'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isUsed;

    @Builder
    public ScreenElement(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId,
        Boolean isDeleted, Integer screenElementNo, String screenElementCd, String screenElementExpln,
        ScreenElementType screenElementTypeCd, ScreenElement upperScreenElement, Integer defaultOrderNo,
        Boolean isDefaultExposed, Boolean isUsed) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.screenElementNo = screenElementNo;
        this.screenElementCd = screenElementCd;
        this.screenElementExpln = screenElementExpln;
        this.screenElementTypeCd = screenElementTypeCd;
        this.upperScreenElement = upperScreenElement;
        this.defaultOrderNo = defaultOrderNo;
        this.isDefaultExposed = isDefaultExposed;
        this.isUsed = isUsed;
    }
}
