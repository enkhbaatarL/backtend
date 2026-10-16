package com.sk.domain.mi.dto.request.fundamentalmodel;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FundamentalModelJobArgs {
    private Long caseId;
    private Long scenarioId;
    private String runId;
    private Long runSeq;
    private String demandExcelPath;
    private String supplyExcelPath;
    private String tradeExcelPath;
    private String outputExcelPath;
    private String callbackUrl;
}
