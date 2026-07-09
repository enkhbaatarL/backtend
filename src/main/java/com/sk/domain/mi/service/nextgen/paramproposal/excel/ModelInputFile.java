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

    DEMAND("Demand", "복호화_(Base)_Demand_Model_Input.xlsm"),
    SUPPLY("Supply", "복호화_(Base)_Supply_Model_Input.xlsm"),
    TRADE("Trade", "복호화_(Base)_Trade_Model_Input.xlsm");

    private final String code;
    private final String basecaseFileName;

    public static ModelInputFile from(String value) {
        return Arrays.stream(values())
            .filter(file -> file.name().equalsIgnoreCase(value) || file.code.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown ModelInputFile: " + value));
    }

}
