package com.sk.domain.mi.exception;

import com.sk.common.enums.ResultCode;
import lombok.Getter;

@Getter
public class ModelParamProposalCreateRejectedException extends RuntimeException {

    private final String failureReason;

    public ModelParamProposalCreateRejectedException(String failureReason) {
        super(ResultCode.Error.API_RESPONSE_ERROR.getMessage());
        this.failureReason = failureReason;
    }
}
