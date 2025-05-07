package com.sk.domain.shared.entity.master.tif;

import java.math.BigDecimal;
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
@Comment("TIF_Futures_수신")
@Table(name = "tif_futuresr")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Futures extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "data_reg_sn", nullable = false)
    @Comment("데이터등록일련번호")
    private Integer dataRegistrationSn;

    @Column(name = "code", nullable = false, length = 30)
    @Comment("원천ID코드")
    private String code;

    @Column(name = "date", nullable = false)
    @Comment("원천전달데이터일시")
    private LocalDateTime date;

    @Column(name = "ps", precision = 15, scale = 4)
    @Comment("정산가격")
    private BigDecimal ps;

    @Column(name = "vm")
    @Comment("거래계약양")
    private Integer vm;

    @Column(name = "oi")
    @Comment("미결제약정")
    private Integer oi;

    @Column(name = "po", precision = 15, scale = 4)
    @Comment("시작가격")
    private BigDecimal po;

    @Column(name = "ph", precision = 15, scale = 4)
    @Comment("당일최고가격")
    private BigDecimal ph;

    @Column(name = "pl", precision = 15, scale = 4)
    @Comment("당일최저가격")
    private BigDecimal pl;

    @Column(name = "modloadyn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("변경적재완료여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isModifiedLoad;

    public void updateIsModifiedLoad() {
        this.isModifiedLoad = true;
    }
}