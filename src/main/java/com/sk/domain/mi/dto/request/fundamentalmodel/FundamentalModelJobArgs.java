package com.sk.domain.mi.dto.request.fundamentalmodel;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FundamentalModelJobArgs {
    private String runId;
    private Long runSeq;
    private String demandExcelPath;
    private String supplyExcelPath;
    private String tradeExcelPath;
    private String callbackUrl;
}
