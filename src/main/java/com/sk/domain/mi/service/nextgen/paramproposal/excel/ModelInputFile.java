package com.sk.domain.mi.service.nextgen.paramproposal.excel;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 수급모델 입력 파일 구분.
 * OUTPUT은 실행 입력이지만 파라미터 제안·확정·병합 대상이 아니다 — 기준 원본 그대로 실행 input에 포함한다.
 */
@Getter
@RequiredArgsConstructor
public enum ModelInputFile {

    DEMAND("Demand", "Demand_v5_플랫폼입력_최종_v8_플랫폼전달.xlsm", true),
    SUPPLY("Supply", "Supply_v14_플랫폼입력_최종_v6_플랫폼전달.xlsm", true),
    TRADE("Trade", "복호화_Trade_v11_플랫폼입력_최종_v8_플랫폼전달.xlsm", true),
    OUTPUT("Output", "Output_v3_플랫폼입력_최종_v8_플랫폼전달.xlsm", false);

    private final String code;
    private final String basecaseFileName;
    private final boolean proposalTarget;

    public static ModelInputFile from(String value) {
        return Arrays.stream(values())
            .filter(file -> file.name().equalsIgnoreCase(value) || file.code.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown ModelInputFile: " + value));
    }

    /** Agent 제안·확정·병합의 modelFile 해석. 제안 대상이 아닌 파일(OUTPUT)은 미지정 값과 동일하게 거절한다. */
    public static ModelInputFile proposalTargetFrom(String value) {
        ModelInputFile file = from(value);
        if (!file.proposalTarget) {
            throw new IllegalArgumentException("ModelInputFile is not a parameter proposal target: " + value);
        }
        return file;
    }

}
