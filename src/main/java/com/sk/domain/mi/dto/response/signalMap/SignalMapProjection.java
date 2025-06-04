package com.sk.domain.mi.dto.response.signalMap;

import java.math.BigDecimal;

public interface SignalMapProjection {
    String getInstmScrnCtgrNm();

    String getInstmScrnDtlCtgrNm();

    String getCorrCalcCycCd();

    String getAnlyFulfDt();

    String getInstmNm();

    String getMnmncCd();

    BigDecimal getCorrCalcVal();
}
