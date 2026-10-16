package com.sk.domain.shared.client.dto.request;

import java.util.List;

/** 파라미터 제안 생성 요청 — 제안 기준 MI Base case와 그 input 4파일 저장 경로. */
public record AgentModelParamProposalCreateReq(

    Data data
) {

    public static AgentModelParamProposalCreateReq of(Long miBaseCaseId, List<InputFile> inputFiles) {
        return new AgentModelParamProposalCreateReq(new Data(miBaseCaseId, inputFiles));
    }

    public record Data(
        Long miBaseCaseId,
        List<InputFile> inputFiles
    ) {
    }

    /** modelFile: Demand | Supply | Trade | Output, storagePath: tcm_filel 저장 경로 원문(URL). */
    public record InputFile(
        String modelFile,
        String storagePath
    ) {
    }
}
