package com.sk.domain.shared.repository.master.tif.custom;

import static com.sk.domain.shared.entity.master.tif.QFuturesSpread.futuresSpread;

import java.time.LocalDateTime;
import java.util.List;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.domain.shared.dto.response.rpa.CrackSpreadDataRes;
import com.sk.domain.shared.dto.response.rpa.QCrackSpreadDataRes;
import com.sk.domain.shared.entity.master.tif.QFutures;
import com.sk.domain.shared.entity.master.tif.QNymexGasolineEnergy;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CustomFuturesSpreadRepositoryImpl implements CustomFuturesSpreadRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public LocalDateTime findLastDateByCode(String code) {
        return queryFactory
            .select(futuresSpread.date.max())
            .from(futuresSpread)
            .where(
                futuresSpread.code.eq(code),
                futuresSpread.isDeleted.isFalse()
            )
            .fetchOne();
    }

    @Override
    public List<CrackSpreadDataRes> calculateCrackSpreadData(LocalDateTime lastDate) {
        QFutures futuresCL = QFutures.futures;
        QFutures futuresHO = new QFutures("futuresHO");
        QNymexGasolineEnergy nymexGasolineEnergy = QNymexGasolineEnergy.nymexGasolineEnergy;

        return queryFactory
            .select(
                new QCrackSpreadDataRes(
                    futuresCL.date,
                    futuresCL.ps,
                    futuresHO.ps,
                    nymexGasolineEnergy.settle,
                    futuresCL.vm,
                    futuresCL.oi,
                    futuresCL.po,
                    futuresCL.ph,
                    futuresCL.pl,
                    nymexGasolineEnergy.totCntrVol,
                    nymexGasolineEnergy.opint1,
                    nymexGasolineEnergy.openPrc,
                    nymexGasolineEnergy.high1,
                    nymexGasolineEnergy.low1
                )
            )
            .from(futuresCL)
            .innerJoin(futuresHO)
            .on(
                futuresCL.date.eq(futuresHO.date),
                futuresHO.code.eq("NHOCS01"),
                futuresHO.isDeleted.isFalse()
            )
            .leftJoin(nymexGasolineEnergy)
            .on(
                futuresCL.date.eq(nymexGasolineEnergy.date),
                nymexGasolineEnergy.code.eq("RBc1"),
                nymexGasolineEnergy.isDeleted.isFalse()
            )
            .where(
                futuresCL.code.eq("NCLC.01"),
                futuresCL.isDeleted.isFalse(),
                lastDate == null ? null : futuresCL.date.gt(lastDate),
                futuresCL.ps.isNotNull(),
                futuresHO.ps.isNotNull(),
                nymexGasolineEnergy.settle.isNotNull()
            )
            .groupBy(futuresCL.date)
            .orderBy(futuresCL.date.asc())
            .fetch();
    }
}