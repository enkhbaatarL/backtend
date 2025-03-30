package com.sk.common.utils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class MathUtil {

    public static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);

    /**
     * 제곱근 함수
     */
    public static BigDecimal sqrt(BigDecimal value, MathContext mc) {
        BigDecimal x = BigDecimal.valueOf(Math.sqrt(value.doubleValue()));
        BigDecimal last;

        do {
            last = x;
            x = value.divide(x, mc).add(x).divide(BigDecimal.valueOf(2), mc);
        } while (!x.equals(last));

        return x;
    }

}
