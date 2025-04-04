package com.sk.common.utils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class MathUtil {

    public static final MathContext MC = new MathContext(15, RoundingMode.HALF_UP);
    public static final int GLOBAL_SCALE = 4;
    public static final RoundingMode GLOBAL_ROUNDING = RoundingMode.HALF_UP;

    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return a.add(b, MC).setScale(GLOBAL_SCALE, GLOBAL_ROUNDING);
    }

    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return a.subtract(b, MC).setScale(GLOBAL_SCALE, GLOBAL_ROUNDING);
    }

    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return a.multiply(b, MC).setScale(GLOBAL_SCALE, GLOBAL_ROUNDING);
    }

    public static BigDecimal divide(BigDecimal a, BigDecimal b) {
        return a.divide(b, MC).setScale(GLOBAL_SCALE, GLOBAL_ROUNDING);
    }

    /**
     * 제곱근 함수
     */
    public static BigDecimal sqrt(BigDecimal value) {
        BigDecimal x = BigDecimal.valueOf(Math.sqrt(value.doubleValue()));
        BigDecimal last;

        do {
            last = x;
            x = divide(add(divide(value, x), x), BigDecimal.valueOf(2));
        } while (!x.equals(last));

        return x;
    }

}
