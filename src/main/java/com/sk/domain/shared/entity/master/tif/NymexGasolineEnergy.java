package com.sk.domain.shared.entity.master.tif;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;

import com.sk.domain.shared.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Getter
@Comment("TIF_NYMEX RBOB Gasoline Electronic Energy Future_수신")
@Table(name = "tif_nymexrbgslnelenfur")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NymexGasolineEnergy extends BaseEntity {

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

    @Column(name = "sequence", nullable = false)
    @Comment("데이터순번")
    private Byte sequence;

    @Column(name = "trdprc_1", precision = 15, scale = 4)
    @Comment("최종거래가격")
    private BigDecimal trdPrc1;

    @Column(name = "open_prc", precision = 15, scale = 4)
    @Comment("당일개장가격")
    private BigDecimal openPrc;

    @Column(name = "high_1", precision = 17, scale = 4)
    @Comment("당일최대가격")
    private BigDecimal high1;

    @Column(name = "low_1", precision = 15, scale = 4)
    @Comment("당일최저가격")
    private BigDecimal low1;

    @Column(name = "acvol_uns")
    @Comment("누적거래량")
    private Long acvolUns;

    @Column(name = "bid", precision = 15, scale = 4)
    @Comment("입찰가격")
    private BigDecimal bid;

    @Column(name = "ask", precision = 15, scale = 4)
    @Comment("매도가격")
    private BigDecimal ask;

    @Column(name = "opint_1", precision = 15, scale = 4)
    @Comment("미결제약정수")
    private BigDecimal opint1;

    @Column(name = "totcntrvol", precision = 15, scale = 4)
    @Comment("상품계약총거래량")
    private Integer totCntrVol;

    @Column(name = "totcntroi", precision = 15, scale = 4)
    @Comment("상품계약총미결제약금액")
    private Integer totCntrOi;

    @Column(name = "settle", precision = 15, scale = 4)
    @Comment("정산가격")
    private BigDecimal settle;

    @Column(name = "num_moves", precision = 15, scale = 4)
    @Comment("체결거래건수")
    private BigDecimal numMoves;

    @Column(name = "mid_price", precision = 15, scale = 4)
    @Comment("매수매도중간가격")
    private BigDecimal midPrice;

    @Column(name = "blkvolum")
    @Comment("누적블록거래량")
    private Integer blkVolum;

    @Column(name = "vol_x_prc1", precision = 15, scale = 4)
    @Comment("최근총거래금액")
    private BigDecimal volXPrc1;

    @Column(name = "ordbk_vol")
    @Comment("누적주문장거래량")
    private Integer ordbkVol;

    @Column(name = "vwap", precision = 15, scale = 4)
    @Comment("볼륨가중평균가격")
    private BigDecimal vwap;

    @Column(name = "uplimit", precision = 15, scale = 4)
    @Comment("당일거래최대한도가격")
    private BigDecimal upLimit;

    @Column(name = "lolimit", precision = 15, scale = 4)
    @Comment("당일거래최저한도가격")
    private BigDecimal loLimit;

    @Column(name = "expir_date")
    @Comment("금융계약만기일시")
    private LocalDateTime expirDate;

    @Column(name = "crt_mnth", length = 10)
    @Comment("계약인도최종월")
    private String crtMnth;

    @Column(name = "setl_pchng", precision = 15, scale = 4)
    @Comment("전일당일정산가격변화율")
    private BigDecimal setlPchng;

    @Column(name = "setl_nchng", precision = 15, scale = 4)
    @Comment("전일당일정산차이가격")
    private BigDecimal setlNchng;
}
