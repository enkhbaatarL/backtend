package com.sk.domain.mi.service.nextgen.paramproposal.excel;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 수급모델 입력 파일 구분.
 */
@Getter
@RequiredArgsConstructor
public enum ModelInputFile {

    DEMAND("Demand", "Demand_v5_플랫폼입력_최종.xlsm"),
    SUPPLY("Supply", "Supply_v14_플랫폼입력_최종.xlsm"),
    TRADE("Trade", "복호화_Trade_v11_플랫폼입력_최종.xlsm");

    private final String code;
    private final String basecaseFileName;

    public static ModelInputFile from(String value) {
        return Arrays.stream(values())
            .filter(file -> file.name().equalsIgnoreCase(value) || file.code.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown ModelInputFile: " + value));
    }

}
