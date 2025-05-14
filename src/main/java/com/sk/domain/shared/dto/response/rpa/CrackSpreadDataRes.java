package com.sk.domain.shared.dto.response.rpa;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.querydsl.core.annotations.QueryProjection;

import io.swagger.v3.oas.annotations.media.Schema;

public record CrackSpreadDataRes(

        @Schema(description = "날짜", example = "2024-01-01 00:00:00")
        LocalDateTime date,

        @Schema(description = "CL 현재가", example = "10000")
        BigDecimal clPs,

        @Schema(description = "HO 현재가", example = "10000")
        BigDecimal hoPs,

        @Schema(description = "RB 종가", example = "10000")
        BigDecimal rbSettle,

        @Schema(description = "CL 매도량", example = "10000")
        Integer clVm,

        @Schema(description = "CL 매도잔량", example = "10000")
        Integer clOi,

        @Schema(description = "CL 매도평균가", example = "10000")
        BigDecimal clPo,

        @Schema(description = "CL 매도평균가", example = "10000")
        BigDecimal clPh,

        @Schema(description = "CL 매도평균가", example = "10000")
        BigDecimal clPl,

        @Schema(description = "RB 총 매도량", example = "10000")
        Integer rbTotCntrVol,

        @Schema(description = "RB 총 매도잔량", example = "10000")
        BigDecimal rbOpint1,

        @Schema(description = "RB 총 매도평균가", example = "10000")
        BigDecimal rbOpenPrc,

        @Schema(description = "RB 총 매도평균가", example = "10000")
        BigDecimal rbHigh1,

        @Schema(description = "RB 총 매도평균가", example = "10000")
        BigDecimal rbLow1) {

    @QueryProjection
    public CrackSpreadDataRes(
            LocalDateTime date,
            BigDecimal clPs,
            BigDecimal hoPs,
            BigDecimal rbSettle,
            Integer clVm,
            Integer clOi,
            BigDecimal clPo,
            BigDecimal clPh,
            BigDecimal clPl,
            Integer rbTotCntrVol,
            BigDecimal rbOpint1,
            BigDecimal rbOpenPrc,
            BigDecimal rbHigh1,
            BigDecimal rbLow1) {
        this.date = date;
        this.clPs = clPs;
        this.hoPs = hoPs;
        this.rbSettle = rbSettle;
        this.clVm = clVm;
        this.clOi = clOi;
        this.clPo = clPo;
        this.clPh = clPh;
        this.clPl = clPl;
        this.rbTotCntrVol = rbTotCntrVol;
        this.rbOpint1 = rbOpint1;
        this.rbOpenPrc = rbOpenPrc;
        this.rbHigh1 = rbHigh1;
        this.rbLow1 = rbLow1;
    }
}
