package com.sk.domain.mi.conversion_on_demand.strategy.basic;

import com.sk.domain.mi.conversion_on_demand.FormulaService;
import com.sk.domain.mi.conversion_on_demand.strategy.ConversionStrategy;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class WaferToChipEaConversion implements ConversionStrategy {

    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);
    private static final BigDecimal PI = new BigDecimal(Math.PI);

    @Override
    public FormulaService getFormulaService() {
        return FormulaService.WAFER_TO_CHIP_EA;
    }

    @Override
    public BigDecimal convert(BigDecimal altitude) {
        // Not implemented
        return null;
    }

    @Override
    public BigDecimal convert(BigDecimal[] values) {

        BigDecimal waferDiameter = values[0];
        BigDecimal chipArea = values[1];

        BigDecimal waferRadius = waferDiameter.divide(new BigDecimal("2"), MC);
        BigDecimal waferArea = PI.multiply(waferRadius.pow(2), MC);

        BigDecimal sqrtTerm = sqrt(chipArea.multiply(new BigDecimal("2"), MC), MC);

        BigDecimal edgeLoss = PI.multiply(waferDiameter, MC).divide(sqrtTerm, MC);
        BigDecimal chips = waferArea.divide(chipArea, MC).subtract(edgeLoss, MC);

        return chips.setScale(0, RoundingMode.FLOOR);
    }

    @Override
    public boolean supports(FormulaService formulaService) {
        return formulaService == getFormulaService();
    }

    /**
     * BigDecimal용 sqrt 함수
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
